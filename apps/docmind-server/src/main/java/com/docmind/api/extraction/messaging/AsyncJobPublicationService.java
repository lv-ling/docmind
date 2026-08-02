package com.docmind.api.extraction.messaging;

import com.docmind.api.extraction.domain.AsyncJob;
import com.docmind.api.extraction.infrastructure.AsyncJobRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsyncJobPublicationService {

  private final AsyncJobRepository jobs;
  private final AsyncJobHandlerRegistry handlers;
  private final AsyncJobProperties properties;
  private final Clock clock;

  public AsyncJobPublicationService(
      AsyncJobRepository jobs,
      AsyncJobHandlerRegistry handlers,
      AsyncJobProperties properties,
      Clock clock) {
    this.jobs = jobs;
    this.handlers = handlers;
    this.properties = properties;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<UUID> findDueJobIds() {
    return jobs.findDispatchCandidateIds(
        clock.instant(), PageRequest.of(0, properties.dispatchBatchSize()));
  }

  @Transactional
  public Optional<ReservedJobPublication> reserve(UUID jobId) {
    AsyncJob job = jobs.findLockedById(jobId).orElse(null);
    if (job == null || !handlers.supports(job.jobType())) {
      return Optional.empty();
    }
    UUID leaseId = job.reservePublication(clock.instant(), properties.publishLease());
    if (leaseId == null) {
      return Optional.empty();
    }
    return Optional.of(new ReservedJobPublication(job.id(), leaseId, AsyncJobCommand.from(job)));
  }

  @Transactional
  public void confirm(ReservedJobPublication publication) {
    jobs
        .findLockedById(publication.jobId())
        .ifPresent(job -> job.confirmPublication(publication.publishLeaseId(), clock.instant()));
  }

  @Transactional
  public void release(ReservedJobPublication publication) {
    Instant now = clock.instant();
    jobs
        .findLockedById(publication.jobId())
        .ifPresent(
            job ->
                job.releasePublication(
                    publication.publishLeaseId(), now, properties.publishRetryDelay()));
  }
}
