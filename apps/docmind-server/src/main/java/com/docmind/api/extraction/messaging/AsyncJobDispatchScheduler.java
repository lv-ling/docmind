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
public class AsyncJobDispatchScheduler {

  private static final Logger log = LoggerFactory.getLogger(AsyncJobDispatchScheduler.class);
  private final AsyncJobPublicationService publications;
  private final RabbitJobPublisher publisher;

  public AsyncJobDispatchScheduler(
      AsyncJobPublicationService publications, RabbitJobPublisher publisher) {
    this.publications = publications;
    this.publisher = publisher;
  }

  @Scheduled(
      initialDelayString = "${docmind.jobs.dispatch-initial-delay:PT5S}",
      fixedDelayString = "${docmind.jobs.dispatch-interval:PT2S}")
  public void dispatchDueJobs() {
    for (UUID jobId : publications.findDueJobIds()) {
      publications
          .reserve(jobId)
          .ifPresent(
              publication -> {
                try {
                  publisher.publish(publication.command());
                  publications.confirm(publication);
                } catch (RabbitJobPublishException exception) {
                  publications.release(publication);
                  log.warn(
                      "async_job_publish_failed job_id={} exception_type={}",
                      publication.jobId(),
                      exception.getClass().getName());
                }
              });
    }
  }
}
