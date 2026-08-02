package com.docmind.api.extraction.messaging;

import com.docmind.api.extraction.domain.AsyncJob;
import com.docmind.api.extraction.domain.AsyncJobType;
import java.time.Instant;
import java.util.UUID;

public record AsyncJobCommand(
    String schemaVersion,
    UUID messageId,
    UUID jobId,
    UUID workspaceId,
    AsyncJobType jobType,
    String aggregateType,
    UUID aggregateId,
    int attempt,
    UUID requestId,
    Instant createdAt) {

  public static final String CURRENT_SCHEMA_VERSION = "1";

  public static AsyncJobCommand from(AsyncJob job) {
    return new AsyncJobCommand(
        CURRENT_SCHEMA_VERSION,
        job.id(),
        job.id(),
        job.workspaceId(),
        job.jobType(),
        job.aggregateType(),
        job.aggregateId(),
        job.attemptCount() + 1,
        job.creationRequestId(),
        job.createdAt());
  }

  public boolean matchesIdentity(AsyncJob job) {
    return CURRENT_SCHEMA_VERSION.equals(schemaVersion)
        && messageId != null
        && messageId.equals(job.id())
        && jobId != null
        && jobId.equals(job.id())
        && workspaceId != null
        && workspaceId.equals(job.workspaceId())
        && jobType == job.jobType()
        && aggregateType != null
        && aggregateType.equals(job.aggregateType())
        && aggregateId != null
        && aggregateId.equals(job.aggregateId())
        && requestId != null
        && requestId.equals(job.creationRequestId())
        && createdAt != null
        && createdAt.equals(job.createdAt())
        && attempt > 0;
  }
}
