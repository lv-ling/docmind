package com.docmind.api.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.docmind.api.extraction.domain.AsyncJobType;
import com.docmind.api.extraction.messaging.AsyncJobCommand;
import com.docmind.api.extraction.messaging.AsyncJobProperties;
import com.docmind.api.extraction.messaging.RabbitJobPublishException;
import com.docmind.api.extraction.messaging.RabbitJobPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitJobPublisherTest {

  private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");

  @Test
  void waitsForBrokerConfirmAndPublishesOnlyIdentifiers() {
    RabbitTemplate template = mock(RabbitTemplate.class);
    AsyncJobProperties properties = RabbitJobTopologyTest.properties();
    RabbitJobPublisher publisher =
        new RabbitJobPublisher(template, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    AsyncJobCommand command = command();
    Message[] publishedMessage = new Message[1];

    doAnswer(
            invocation -> {
              MessagePostProcessor processor = invocation.getArgument(3);
              CorrelationData correlation = invocation.getArgument(4);
              publishedMessage[0] =
                  processor.postProcessMessage(
                      new Message(new byte[0], new MessageProperties()));
              correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
              return null;
            })
        .when(template)
        .convertAndSend(
            eq(properties.exchange()),
            eq(properties.routingKey()),
            eq(command),
            any(MessagePostProcessor.class),
            any(CorrelationData.class));

    publisher.publish(command);

    assertThat(publishedMessage[0].getMessageProperties().getMessageId())
        .isEqualTo(command.messageId().toString());
    assertThat(publishedMessage[0].getMessageProperties().getCorrelationId())
        .isEqualTo(command.requestId().toString());
    assertThat(publishedMessage[0].getMessageProperties().getDeliveryMode())
        .isEqualTo(MessageDeliveryMode.PERSISTENT);
  }

  @Test
  void failsWhenBrokerRejectsPublication() {
    RabbitTemplate template = mock(RabbitTemplate.class);
    AsyncJobProperties properties = RabbitJobTopologyTest.properties();
    RabbitJobPublisher publisher =
        new RabbitJobPublisher(template, properties, Clock.fixed(NOW, ZoneOffset.UTC));

    doAnswer(
            invocation -> {
              CorrelationData correlation = invocation.getArgument(4);
              correlation.getFuture().complete(new CorrelationData.Confirm(false, "nack"));
              return null;
            })
        .when(template)
        .convertAndSend(
            eq(properties.exchange()),
            eq(properties.routingKey()),
            any(AsyncJobCommand.class),
            any(MessagePostProcessor.class),
            any(CorrelationData.class));

    assertThatThrownBy(() -> publisher.publish(command()))
        .isInstanceOf(RabbitJobPublishException.class);
  }

  private AsyncJobCommand command() {
    UUID jobId = UUID.randomUUID();
    return new AsyncJobCommand(
        AsyncJobCommand.CURRENT_SCHEMA_VERSION,
        jobId,
        jobId,
        UUID.randomUUID(),
        AsyncJobType.EXTRACTION,
        "extraction_run",
        UUID.randomUUID(),
        1,
        UUID.randomUUID(),
        NOW);
  }
}
