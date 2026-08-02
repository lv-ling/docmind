package com.docmind.api.extraction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "async_job",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_async_job_aggregate",
            columnNames = {"aggregate_type", "aggregate_id"}))
public class AsyncJob {

  @Id private UUID id;

  @Column(name = "workspace_id", nullable = false, updatable = false)
  private UUID workspaceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "job_type", nullable = false, updatable = false, length = 40)
  private AsyncJobType jobType;

  @Column(name = "aggregate_type", nullable = false, updatable = false, length = 80)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false, updatable = false)
  private UUID aggregateId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AsyncJobStatus status;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "max_attempts", nullable = false, updatable = false)
  private int maxAttempts;

  @Column(name = "available_at", nullable = false)
  private Instant availableAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "failure_code", length = 100)
  private String failureCode;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "publish_attempt_count", nullable = false)
  private int publishAttemptCount;

  @Column(name = "last_publish_attempt_at")
  private Instant lastPublishAttemptAt;

  @Column(name = "publish_lease_id")
  private UUID publishLeaseId;

  @Column(name = "publish_lease_expires_at")
  private Instant publishLeaseExpiresAt;

  @Column(name = "worker_lease_expires_at")
  private Instant workerLeaseExpiresAt;

  @Version
  @Column(nullable = false)
  private long revision;

  @Column(name = "creation_request_id", nullable = false, updatable = false)
  private UUID creationRequestId;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected AsyncJob() {}

  public AsyncJob(
      UUID id,
      UUID workspaceId,
      AsyncJobType jobType,
      String aggregateType,
      UUID aggregateId,
      int maxAttempts,
      UUID creationRequestId,
      UUID createdBy,
      Instant now) {
    this.id = id;
    this.workspaceId = workspaceId;
    this.jobType = jobType;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.status = AsyncJobStatus.QUEUED;
    this.maxAttempts = maxAttempts;
    this.availableAt = now;
    this.creationRequestId = creationRequestId;
    this.createdBy = createdBy;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public boolean isPublishable(Instant now) {
    return (status == AsyncJobStatus.QUEUED || status == AsyncJobStatus.RETRYING)
        && !availableAt.isAfter(now)
        && publishedAt == null
        && (publishLeaseExpiresAt == null || !publishLeaseExpiresAt.isAfter(now));
  }

  public UUID reservePublication(Instant now, Duration leaseDuration) {
    if (!isPublishable(now)) {
      return null;
    }
    publishLeaseId = UUID.randomUUID();
    publishLeaseExpiresAt = now.plus(leaseDuration);
    lastPublishAttemptAt = now;
    publishAttemptCount++;
    updatedAt = now;
    return publishLeaseId;
  }

  public boolean confirmPublication(UUID leaseId, Instant now) {
    if (!ownsPublishLease(leaseId)) {
      return false;
    }
    publishedAt = now;
    clearPublishLease();
    updatedAt = now;
    return true;
  }

  public boolean releasePublication(UUID leaseId, Instant now, Duration retryDelay) {
    if (!ownsPublishLease(leaseId)) {
      return false;
    }
    clearPublishLease();
    availableAt = now.plus(retryDelay);
    updatedAt = now;
    return true;
  }

  public boolean claimForExecution(Instant now, Duration leaseDuration) {
    if ((status != AsyncJobStatus.QUEUED && status != AsyncJobStatus.RETRYING)
        || availableAt.isAfter(now)
        || publishedAt == null
        || attemptCount >= maxAttempts) {
      return false;
    }
    status = AsyncJobStatus.RUNNING;
    attemptCount++;
    startedAt = now;
    completedAt = null;
    workerLeaseExpiresAt = now.plus(leaseDuration);
    updatedAt = now;
    return true;
  }

  public void acceptBrokerDelivery(Instant now) {
    if (publishedAt == null) {
      publishedAt = now;
      if (availableAt.isAfter(now)) {
        availableAt = now;
      }
      clearPublishLease();
      updatedAt = now;
    }
  }

  public void markSucceeded(Instant now) {
    requireRunning();
    status = AsyncJobStatus.SUCCEEDED;
    failureCode = null;
    completedAt = now;
    workerLeaseExpiresAt = null;
    updatedAt = now;
  }

  public boolean markExecutionFailed(
      String safeFailureCode, Instant now, Duration retryDelay, boolean retryable) {
    requireRunning();
    failureCode = safeFailureCode;
    workerLeaseExpiresAt = null;
    updatedAt = now;
    if (retryable && attemptCount < maxAttempts) {
      status = AsyncJobStatus.RETRYING;
      availableAt = now.plus(retryDelay);
      publishedAt = null;
      completedAt = null;
      return true;
    }
    status = AsyncJobStatus.FAILED;
    completedAt = now;
    return false;
  }

  public boolean recoverExpiredWorkerLease(
      Instant now, Duration retryDelay, String safeFailureCode) {
    if (status != AsyncJobStatus.RUNNING
        || workerLeaseExpiresAt == null
        || workerLeaseExpiresAt.isAfter(now)) {
      return false;
    }
    markExecutionFailed(safeFailureCode, now, retryDelay, true);
    return true;
  }

  private boolean ownsPublishLease(UUID leaseId) {
    return leaseId != null && leaseId.equals(publishLeaseId);
  }

  private void clearPublishLease() {
    publishLeaseId = null;
    publishLeaseExpiresAt = null;
  }

  private void requireRunning() {
    if (status != AsyncJobStatus.RUNNING) {
      throw new IllegalStateException("Async job is not running");
    }
  }

  public UUID id() { return id; }
  public UUID workspaceId() { return workspaceId; }
  public AsyncJobType jobType() { return jobType; }
  public String aggregateType() { return aggregateType; }
  public UUID aggregateId() { return aggregateId; }
  public AsyncJobStatus status() { return status; }
  public int attemptCount() { return attemptCount; }
  public int maxAttempts() { return maxAttempts; }
  public Instant availableAt() { return availableAt; }
  public Instant startedAt() { return startedAt; }
  public Instant completedAt() { return completedAt; }
  public String failureCode() { return failureCode; }
  public Instant publishedAt() { return publishedAt; }
  public int publishAttemptCount() { return publishAttemptCount; }
  public Instant lastPublishAttemptAt() { return lastPublishAttemptAt; }
  public UUID publishLeaseId() { return publishLeaseId; }
  public Instant publishLeaseExpiresAt() { return publishLeaseExpiresAt; }
  public Instant workerLeaseExpiresAt() { return workerLeaseExpiresAt; }
  public UUID creationRequestId() { return creationRequestId; }
  public UUID createdBy() { return createdBy; }
  public Instant createdAt() { return createdAt; }
}
