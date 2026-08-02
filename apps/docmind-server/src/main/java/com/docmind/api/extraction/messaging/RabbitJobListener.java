package com.docmind.api.extraction.messaging;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "docmind.jobs.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class RabbitJobListener {

  private static final Logger log = LoggerFactory.getLogger(RabbitJobListener.class);
  private final AsyncJobHandlerRegistry handlers;
  private final AsyncJobExecutionStateService states;

  public RabbitJobListener(
      AsyncJobHandlerRegistry handlers, AsyncJobExecutionStateService states) {
    this.handlers = handlers;
    this.states = states;
  }

  @RabbitListener(
      queues = "${docmind.jobs.queue:docmind.jobs.execute.v1}",
      containerFactory = "asyncJobRabbitListenerContainerFactory",
      autoStartup = "${docmind.jobs.consumer-enabled:true}")
  public void receive(AsyncJobCommand command, Message message, Channel channel)
      throws IOException {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    AsyncJobHandler handler =
        command == null || command.jobType() == null
            ? null
            : handlers.find(command.jobType()).orElse(null);
    if (handler == null) {
      reject(channel, deliveryTag);
      return;
    }

    JobClaimDecision claim;
    try {
      claim = states.claim(command);
    } catch (RuntimeException exception) {
      logInfrastructureFailure("async_job_claim_failed", command, exception);
      requeue(channel, deliveryTag);
      return;
    }

    switch (claim) {
      case ACKNOWLEDGE_DUPLICATE -> acknowledge(channel, deliveryTag);
      case REQUEUE_NOT_DUE -> requeue(channel, deliveryTag);
      case REJECT_INVALID -> reject(channel, deliveryTag);
      case CLAIMED -> execute(handler, command, channel, deliveryTag);
    }
  }

  private void execute(
      AsyncJobHandler handler, AsyncJobCommand command, Channel channel, long deliveryTag)
      throws IOException {
    try {
      handler.handle(command);
    } catch (AsyncJobExecutionException exception) {
      finishFailure(
          handler, command, exception.failureCode(), exception.retryable(), channel, deliveryTag);
      return;
    } catch (RuntimeException exception) {
      log.warn(
          "async_job_handler_failed job_id={} job_type={} exception_type={}",
          command.jobId(),
          command.jobType(),
          exception.getClass().getName());
      finishFailure(handler, command, "UNEXPECTED_JOB_FAILURE", true, channel, deliveryTag);
      return;
    }

    try {
      states.markSucceeded(command.jobId());
      acknowledge(channel, deliveryTag);
    } catch (RuntimeException exception) {
      logInfrastructureFailure("async_job_completion_failed", command, exception);
      requeue(channel, deliveryTag);
    }
  }

  private void finishFailure(
      AsyncJobHandler handler,
      AsyncJobCommand command,
      String failureCode,
      boolean retryable,
      Channel channel,
      long deliveryTag)
      throws IOException {
    try {
      JobFailureDecision decision =
          states.markFailed(command.jobId(), failureCode, retryable);
      if (decision == JobFailureDecision.TERMINAL_FAILURE) {
        notifyTerminalFailure(handler, command, failureCode);
        reject(channel, deliveryTag);
      } else {
        notifyRetryScheduled(handler, command, failureCode);
        acknowledge(channel, deliveryTag);
      }
    } catch (RuntimeException exception) {
      logInfrastructureFailure("async_job_failure_record_failed", command, exception);
      requeue(channel, deliveryTag);
    }
  }

  private void notifyRetryScheduled(
      AsyncJobHandler handler, AsyncJobCommand command, String failureCode) {
    try {
      handler.onRetryScheduled(command, failureCode);
    } catch (RuntimeException exception) {
      logInfrastructureFailure("async_job_retry_callback_failed", command, exception);
    }
  }

  private void notifyTerminalFailure(
      AsyncJobHandler handler, AsyncJobCommand command, String failureCode) {
    try {
      handler.onTerminalFailure(command, failureCode);
    } catch (RuntimeException exception) {
      logInfrastructureFailure("async_job_terminal_callback_failed", command, exception);
    }
  }

  private void logInfrastructureFailure(
      String event, AsyncJobCommand command, RuntimeException exception) {
    log.warn(
        "{} job_id={} job_type={} exception_type={}",
        event,
        command == null ? null : command.jobId(),
        command == null ? null : command.jobType(),
        exception.getClass().getName());
  }

  private void acknowledge(Channel channel, long deliveryTag) throws IOException {
    channel.basicAck(deliveryTag, false);
  }

  private void requeue(Channel channel, long deliveryTag) throws IOException {
    channel.basicNack(deliveryTag, false, true);
  }

  private void reject(Channel channel, long deliveryTag) throws IOException {
    channel.basicReject(deliveryTag, false);
  }
}
