package com.docmind.api.extraction.infrastructure;

import com.docmind.api.extraction.domain.AsyncJob;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AsyncJobRepository extends JpaRepository<AsyncJob, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select job from AsyncJob job where job.id = :jobId")
  Optional<AsyncJob> findLockedById(@Param("jobId") UUID jobId);

  @Query(
      """
      select job.id from AsyncJob job
      where job.status in (com.docmind.api.extraction.domain.AsyncJobStatus.QUEUED,
                           com.docmind.api.extraction.domain.AsyncJobStatus.RETRYING)
        and job.availableAt <= :now
        and job.publishedAt is null
        and (job.publishLeaseExpiresAt is null or job.publishLeaseExpiresAt <= :now)
      order by job.createdAt asc
      """)
  List<UUID> findDispatchCandidateIds(@Param("now") Instant now, Pageable pageable);

  @Query(
      """
      select job.id from AsyncJob job
      where job.status = com.docmind.api.extraction.domain.AsyncJobStatus.RUNNING
        and job.workerLeaseExpiresAt <= :now
      order by job.workerLeaseExpiresAt asc
      """)
  List<UUID> findExpiredWorkerLeaseIds(@Param("now") Instant now, Pageable pageable);
}
