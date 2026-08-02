package com.docmind.api.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.docmind.api.extraction.domain.AsyncJob;
import com.docmind.api.extraction.domain.AsyncJobStatus;
import com.docmind.api.extraction.domain.AsyncJobType;
import com.docmind.api.extraction.infrastructure.AsyncJobRepository;
import com.docmind.api.extraction.messaging.AsyncJobCommand;
import com.docmind.api.extraction.messaging.AsyncJobExecutionStateService;
import com.docmind.api.extraction.messaging.AsyncJobProperties;
import com.docmind.api.extraction.messaging.JobClaimDecision;
import com.docmind.api.extraction.messaging.JobFailureDecision;
import com.docmind.api.extraction.messaging.RetryBackoffPolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsyncJobExecutionStateServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");

  private AsyncJobRepository jobs;
  private AsyncJobExecutionStateService states;

  @BeforeEach
  void setUp() {
    jobs = mock(AsyncJobRepository.class);
    AsyncJobProperties properties = RabbitJobTopologyTest.properties();
    states =
        new AsyncJobExecutionStateService(
            jobs,
            properties,
            new RetryBackoffPolicy(properties),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void brokerDeliveryClosesPublisherCrashWindowAndClaimsOnce() {
    AsyncJob job = newJob();
    AsyncJobCommand command = AsyncJobCommand.from(job);
    when(jobs.findLockedById(job.id())).thenReturn(Optional.of(job));

    assertThat(states.claim(command)).isEqualTo(JobClaimDecision.CLAIMED);
    assertThat(job.publishedAt()).isEqualTo(NOW);
    assertThat(job.status()).isEqualTo(AsyncJobStatus.RUNNING);
    assertThat(job.attemptCount()).isEqualTo(1);

    assertThat(states.claim(command)).isEqualTo(JobClaimDecision.ACKNOWLEDGE_DUPLICATE);
  }

  @Test
  void recordsRetryWithoutPersistingExceptionMessages() {
    AsyncJob job = newJob();
    AsyncJobCommand command = AsyncJobCommand.from(job);
    when(jobs.findLockedById(job.id())).thenReturn(Optional.of(job));
    states.claim(command);

    assertThat(states.markFailed(job.id(), "AI_TIMEOUT", true))
        .isEqualTo(JobFailureDecision.RETRY_SCHEDULED);
    assertThat(job.status()).isEqualTo(AsyncJobStatus.RETRYING);
    assertThat(job.failureCode()).isEqualTo("AI_TIMEOUT");
    assertThat(job.availableAt()).isEqualTo(NOW.plusSeconds(5));
  }

  @Test
  void rejectsCommandWhoseWorkspaceDoesNotMatchPersistedJob() {
    AsyncJob job = newJob();
    AsyncJobCommand valid = AsyncJobCommand.from(job);
    AsyncJobCommand invalid =
        new AsyncJobCommand(
            valid.schemaVersion(),
            valid.messageId(),
            valid.jobId(),
            UUID.randomUUID(),
            valid.jobType(),
            valid.aggregateType(),
            valid.aggregateId(),
            valid.attempt(),
            valid.requestId(),
            valid.createdAt());
    when(jobs.findLockedById(job.id())).thenReturn(Optional.of(job));

    assertThat(states.claim(invalid)).isEqualTo(JobClaimDecision.REJECT_INVALID);
    assertThat(job.status()).isEqualTo(AsyncJobStatus.QUEUED);
  }

  private AsyncJob newJob() {
    return new AsyncJob(
        UUID.randomUUID(),
        UUID.randomUUID(),
        AsyncJobType.EXTRACTION,
        "extraction_run",
        UUID.randomUUID(),
        3,
        UUID.randomUUID(),
        UUID.randomUUID(),
        NOW);
  }
}
