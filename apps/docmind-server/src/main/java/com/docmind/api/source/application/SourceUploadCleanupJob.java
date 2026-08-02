package com.docmind.api.source.application;

import com.docmind.api.infrastructure.storage.ObjectStorage;
import com.docmind.api.infrastructure.storage.ObjectStorageException;
import com.docmind.api.source.domain.SourceUploadSession;
import com.docmind.api.source.domain.SourceVersion;
import com.docmind.api.source.domain.UploadSessionStatus;
import com.docmind.api.source.infrastructure.SourceUploadSessionRepository;
import com.docmind.api.source.infrastructure.SourceVersionRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
    name = "docmind.storage.upload-cleanup-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SourceUploadCleanupJob {

  private static final Logger log = LoggerFactory.getLogger(SourceUploadCleanupJob.class);
  private final SourceUploadSessionRepository uploads;
  private final SourceVersionRepository versions;
  private final ObjectStorage storage;
  private final Clock clock;

  public SourceUploadCleanupJob(
      SourceUploadSessionRepository uploads,
      SourceVersionRepository versions,
      ObjectStorage storage,
      Clock clock) {
    this.uploads = uploads;
    this.versions = versions;
    this.storage = storage;
    this.clock = clock;
  }

  @Scheduled(
      initialDelayString = "${docmind.storage.upload-cleanup-initial-delay:PT1M}",
      fixedDelayString = "${docmind.storage.upload-cleanup-interval:PT1M}")
  @Transactional
  public void expireAbandonedUploads() {
    java.time.Instant now = clock.instant();
    java.util.List<SourceUploadSession> expired =
        uploads.findTop100ByStagingCleanedAtIsNullAndExpiresAtBeforeOrderByExpiresAtAsc(now);
    for (SourceUploadSession upload : expired) {
      SourceVersion version = versions.findById(upload.sourceVersionId()).orElse(null);
      if (upload.status() == UploadSessionStatus.PENDING
          || upload.status() == UploadSessionStatus.UPLOADING) {
        upload.expire();
      }
      if (version != null) {
        try {
          storage.delete(version.uploadBucket(), version.uploadKey());
          upload.markStagingCleaned(now);
        } catch (ObjectStorageException exception) {
          log.warn(
              "expired_source_upload_cleanup_failed exception_type={}",
              exception.getCause() == null
                  ? exception.getClass().getName()
                  : exception.getCause().getClass().getName());
        }
      } else {
        upload.markStagingCleaned(now);
      }
    }
  }
}
