package com.docmind.api.extraction.messaging;

import com.docmind.api.extraction.domain.AsyncJob;
import com.docmind.api.extraction.domain.AsyncJobStatus;
import com.docmind.api.extraction.infrastructure.AsyncJobRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsyncJobExecutionStateService {

  private static final String WORKER_LEASE_EXPIRED = "WORKER_LEASE_EXPIRED";

  private final AsyncJobRepository jobs;
  private final AsyncJobProperties properties;
  private final RetryBackoffPolicy retryBackoff;
  private final Clock clock;

  public AsyncJobExecutionStateService(
      AsyncJobRepository jobs,
      AsyncJobProperties properties,
      RetryBackoffPolicy retryBackoff,
      Clock clock) {
    this.jobs = jobs;
    this.properties = properties;
    this.retryBackoff = retryBackoff;
    this.clock = clock;
  }

  @Transactional
  public JobClaimDecision claim(AsyncJobCommand command) {
    if (command == null || command.jobId() == null) {
      return JobClaimDecision.REJECT_INVALID;
    }
    AsyncJob job = jobs.findLockedById(command.jobId()).orElse(null);
    if (job == null || !command.matchesIdentity(job)) {
      return JobClaimDecision.REJECT_INVALID;
    }
    if (job.status() == AsyncJobStatus.SUCCEEDED || job.status() == AsyncJobStatus.FAILED) {
      return JobClaimDecision.ACKNOWLEDGE_DUPLICATE;
    }
    if (command.attempt() <= job.attemptCount()) {
      return JobClaimDecision.ACKNOWLEDGE_DUPLICATE;
    }
    if (command.attempt() != job.attemptCount() + 1) {
      return JobClaimDecision.REJECT_INVALID;
    }

    Instant now = clock.instant();
    job.acceptBrokerDelivery(now);
    return job.claimForExecution(now, properties.workerLease())
        ? JobClaimDecision.CLAIMED
        : JobClaimDecision.REQUEUE_NOT_DUE;
  }

  @Transactional
  public boolean markSucceeded(UUID jobId) {
    AsyncJob job = jobs.findLockedById(jobId).orElse(null);
    if (job == null || job.status() != AsyncJobStatus.RUNNING) {
      return false;
    }
    job.markSucceeded(clock.instant());
    return true;
  }

  @Transactional
  public JobFailureDecision markFailed(UUID jobId, String failureCode, boolean retryable) {
    AsyncJob job = jobs.findLockedById(jobId).orElse(null);
    if (job == null || job.status() != AsyncJobStatus.RUNNING) {
      return JobFailureDecision.ALREADY_FINALIZED;
    }
    Duration delay = retryBackoff.delayAfterAttempt(job.attemptCount());
    boolean scheduled =
        job.markExecutionFailed(failureCode, clock.instant(), delay, retryable);
    return scheduled
        ? JobFailureDecision.RETRY_SCHEDULED
        : JobFailureDecision.TERMINAL_FAILURE;
  }

  @Transactional(readOnly = true)
  public List<UUID> findExpiredWorkerLeaseIds() {
    return jobs.findExpiredWorkerLeaseIds(
        clock.instant(), PageRequest.of(0, properties.dispatchBatchSize()));
  }

  @Transactional
  public JobFailureDecision recoverExpiredWorkerLease(UUID jobId) {
    AsyncJob job = jobs.findLockedById(jobId).orElse(null);
    if (job == null || job.status() != AsyncJobStatus.RUNNING) {
      return JobFailureDecision.ALREADY_FINALIZED;
    }
    Instant now = clock.instant();
    Duration delay = retryBackoff.delayAfterAttempt(job.attemptCount());
    if (!job.recoverExpiredWorkerLease(now, delay, WORKER_LEASE_EXPIRED)) {
      return JobFailureDecision.ALREADY_FINALIZED;
    }
    return job.status() == AsyncJobStatus.RETRYING
        ? JobFailureDecision.RETRY_SCHEDULED
        : JobFailureDecision.TERMINAL_FAILURE;
  }
}
