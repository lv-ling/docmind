package com.docmind.api.extraction.messaging;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = {"docmind.jobs.enabled", "docmind.jobs.dispatcher-enabled"},
    havingValue = "true",
    matchIfMissing = true)
public class AsyncJobRecoveryScheduler {

  private static final Logger log = LoggerFactory.getLogger(AsyncJobRecoveryScheduler.class);
  private final AsyncJobExecutionStateService states;

  public AsyncJobRecoveryScheduler(AsyncJobExecutionStateService states) {
    this.states = states;
  }

  @Scheduled(
      initialDelayString = "${docmind.jobs.recovery-interval:PT30S}",
      fixedDelayString = "${docmind.jobs.recovery-interval:PT30S}")
  public void recoverExpiredWorkerLeases() {
    for (UUID jobId : states.findExpiredWorkerLeaseIds()) {
      JobFailureDecision decision = states.recoverExpiredWorkerLease(jobId);
      log.warn("async_job_worker_lease_recovered job_id={} outcome={}", jobId, decision);
    }
  }
}
