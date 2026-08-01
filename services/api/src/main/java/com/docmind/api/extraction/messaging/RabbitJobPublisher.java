package com.docmind.api.extraction.messaging;

import java.time.Clock;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "docmind.jobs.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class RabbitJobPublisher {

  private final RabbitTemplate rabbitTemplate;
  private final AsyncJobProperties properties;
  private final Clock clock;

  public RabbitJobPublisher(
      RabbitTemplate rabbitTemplate, AsyncJobProperties properties, Clock clock) {
    this.rabbitTemplate = rabbitTemplate;
    this.properties = properties;
    this.clock = clock;
  }

  public void publish(AsyncJobCommand command) {
    CorrelationData correlation = new CorrelationData(command.messageId().toString());
    rabbitTemplate.convertAndSend(
        properties.exchange(),
        properties.routingKey(),
        command,
        message -> {
          message.getMessageProperties().setMessageId(command.messageId().toString());
          message.getMessageProperties().setCorrelationId(command.requestId().toString());
          message.getMessageProperties().setType("docmind.async-job.v1");
          message.getMessageProperties().setTimestamp(Date.from(clock.instant()));
          message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
          message
              .getMessageProperties()
              .setHeader("x-docmind-job-id", command.jobId().toString());
          message
              .getMessageProperties()
              .setHeader("x-docmind-schema-version", command.schemaVersion());
          return message;
        },
        correlation);

    try {
      CorrelationData.Confirm confirm =
          correlation
              .getFuture()
              .get(properties.publishConfirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
      if (!confirm.isAck() || correlation.getReturned() != null) {
        throw new RabbitJobPublishException();
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RabbitJobPublishException(exception);
    } catch (ExecutionException | TimeoutException exception) {
      throw new RabbitJobPublishException(exception);
    }
  }
}
