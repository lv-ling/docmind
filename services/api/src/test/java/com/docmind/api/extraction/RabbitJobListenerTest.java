package com.docmind.api.extraction;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.docmind.api.extraction.domain.AsyncJobType;
import com.docmind.api.extraction.messaging.AsyncJobCommand;
import com.docmind.api.extraction.messaging.AsyncJobExecutionException;
import com.docmind.api.extraction.messaging.AsyncJobExecutionStateService;
import com.docmind.api.extraction.messaging.AsyncJobHandler;
import com.docmind.api.extraction.messaging.AsyncJobHandlerRegistry;
import com.docmind.api.extraction.messaging.JobClaimDecision;
import com.docmind.api.extraction.messaging.JobFailureDecision;
import com.docmind.api.extraction.messaging.RabbitJobListener;
import com.rabbitmq.client.Channel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class RabbitJobListenerTest {

  private AsyncJobHandler handler;
  private AsyncJobExecutionStateService states;
  private RabbitJobListener listener;
  private Channel channel;
  private Message message;
  private AsyncJobCommand command;

  @BeforeEach
  void setUp() {
    handler = mock(AsyncJobHandler.class);
    states = mock(AsyncJobExecutionStateService.class);
    channel = mock(Channel.class);
    when(handler.jobType()).thenReturn(AsyncJobType.EXTRACTION);
    listener = new RabbitJobListener(new AsyncJobHandlerRegistry(List.of(handler)), states);
    MessageProperties messageProperties = new MessageProperties();
    messageProperties.setDeliveryTag(7L);
    message = new Message(new byte[0], messageProperties);
    UUID jobId = UUID.randomUUID();
    command =
        new AsyncJobCommand(
            AsyncJobCommand.CURRENT_SCHEMA_VERSION,
            jobId,
            jobId,
            UUID.randomUUID(),
            AsyncJobType.EXTRACTION,
            "extraction_run",
            UUID.randomUUID(),
            1,
            UUID.randomUUID(),
            Instant.parse("2026-08-01T00:00:00Z"));
  }

  @Test
  void acknowledgesSuccessfulExecution() throws Exception {
    when(states.claim(command)).thenReturn(JobClaimDecision.CLAIMED);
    when(states.markSucceeded(command.jobId())).thenReturn(true);

    listener.receive(command, message, channel);

    verify(handler).handle(command);
    verify(states).markSucceeded(command.jobId());
    verify(channel).basicAck(7L, false);
  }

  @Test
  void schedulesRetryAndAcknowledgesCurrentDelivery() throws Exception {
    when(states.claim(command)).thenReturn(JobClaimDecision.CLAIMED);
    org.mockito.Mockito.doThrow(
            new AsyncJobExecutionException("AI_TIMEOUT", true, null))
        .when(handler)
        .handle(command);
    when(states.markFailed(command.jobId(), "AI_TIMEOUT", true))
        .thenReturn(JobFailureDecision.RETRY_SCHEDULED);

    listener.receive(command, message, channel);

    verify(channel).basicAck(7L, false);
  }

  @Test
  void rejectsTerminalFailureToDeadLetterQueue() throws Exception {
    when(states.claim(command)).thenReturn(JobClaimDecision.CLAIMED);
    org.mockito.Mockito.doThrow(
            new AsyncJobExecutionException("INVALID_MODEL_OUTPUT", false, null))
        .when(handler)
        .handle(command);
    when(states.markFailed(command.jobId(), "INVALID_MODEL_OUTPUT", false))
        .thenReturn(JobFailureDecision.TERMINAL_FAILURE);

    listener.receive(command, message, channel);

    verify(channel).basicReject(7L, false);
  }

  @Test
  void acknowledgesDuplicateWithoutExecutingHandler() throws Exception {
    when(states.claim(command)).thenReturn(JobClaimDecision.ACKNOWLEDGE_DUPLICATE);

    listener.receive(command, message, channel);

    verify(channel).basicAck(7L, false);
    verify(handler, org.mockito.Mockito.never()).handle(command);
  }
}
