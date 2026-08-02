package com.docmind.api.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.docmind.api.extraction.domain.AsyncJob;
import com.docmind.api.extraction.domain.AsyncJobStatus;
import com.docmind.api.extraction.domain.AsyncJobType;
import com.docmind.api.extraction.messaging.AsyncJobCommand;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AsyncJobLifecycleTest {

  private static final Instant CREATED_AT = Instant.parse("2026-08-01T00:00:00Z");

  @Test
  void confirmsPublicationAndRetriesWithANewAttempt() {
    AsyncJob job = newJob(3);

    UUID firstPublication = job.reservePublication(CREATED_AT, Duration.ofSeconds(30));
    assertThat(firstPublication).isNotNull();
    assertThat(job.publishAttemptCount()).isEqualTo(1);
    assertThat(AsyncJobCommand.from(job).attempt()).isEqualTo(1);

    job.confirmPublication(firstPublication, CREATED_AT.plusSeconds(1));
    assertThat(job.publishedAt()).isEqualTo(CREATED_AT.plusSeconds(1));
    assertThat(job.claimForExecution(CREATED_AT.plusSeconds(2), Duration.ofMinutes(2)))
        .isTrue();
    assertThat(job.status()).isEqualTo(AsyncJobStatus.RUNNING);
    assertThat(job.attemptCount()).isEqualTo(1);

    assertThat(
            job.markExecutionFailed(
                "AI_TIMEOUT", CREATED_AT.plusSeconds(3), Duration.ofSeconds(5), true))
        .isTrue();
    assertThat(job.status()).isEqualTo(AsyncJobStatus.RETRYING);
    assertThat(job.availableAt()).isEqualTo(CREATED_AT.plusSeconds(8));
    assertThat(job.publishedAt()).isNull();

    UUID secondPublication =
        job.reservePublication(CREATED_AT.plusSeconds(8), Duration.ofSeconds(30));
    assertThat(secondPublication).isNotNull();
    assertThat(AsyncJobCommand.from(job).attempt()).isEqualTo(2);
  }

  @Test
  void movesNonRetryableFailureDirectlyToDeadLetterTerminalState() {
    AsyncJob job = newJob(3);
    UUID publication = job.reservePublication(CREATED_AT, Duration.ofSeconds(30));
    job.confirmPublication(publication, CREATED_AT.plusSeconds(1));
    job.claimForExecution(CREATED_AT.plusSeconds(2), Duration.ofMinutes(2));

    assertThat(
            job.markExecutionFailed(
                "INVALID_JOB_PAYLOAD", CREATED_AT.plusSeconds(3), Duration.ofSeconds(5), false))
        .isFalse();
    assertThat(job.status()).isEqualTo(AsyncJobStatus.FAILED);
    assertThat(job.failureCode()).isEqualTo("INVALID_JOB_PAYLOAD");
    assertThat(job.completedAt()).isEqualTo(CREATED_AT.plusSeconds(3));
    assertThat(job.workerLeaseExpiresAt()).isNull();
  }

  @Test
  void ignoresStalePublicationOwners() {
    AsyncJob job = newJob(3);
    UUID publication = job.reservePublication(CREATED_AT, Duration.ofSeconds(30));

    assertThat(job.confirmPublication(UUID.randomUUID(), CREATED_AT.plusSeconds(1))).isFalse();
    assertThat(job.releasePublication(UUID.randomUUID(), CREATED_AT, Duration.ofSeconds(5)))
        .isFalse();
    assertThat(job.confirmPublication(publication, CREATED_AT.plusSeconds(1))).isTrue();
  }

  private AsyncJob newJob(int maxAttempts) {
    return new AsyncJob(
        UUID.randomUUID(),
        UUID.randomUUID(),
        AsyncJobType.EXTRACTION,
        "extraction_run",
        UUID.randomUUID(),
        maxAttempts,
        UUID.randomUUID(),
        UUID.randomUUID(),
        CREATED_AT);
  }
}
