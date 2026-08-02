package com.docmind.api.audit.application;

import com.docmind.api.audit.domain.AuditEvent;
import com.docmind.api.audit.domain.AuditOutcome;
import com.docmind.api.audit.infrastructure.AuditEventRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditRecorder {

  private final AuditEventRepository repository;

  public AuditRecorder(AuditEventRepository repository) {
    this.repository = repository;
  }

  public void record(
      UUID workspaceId,
      UUID actorUserId,
      String action,
      String targetType,
      UUID targetId,
      AuditOutcome outcome,
      UUID requestId,
      Map<String, Object> metadata) {
    repository.save(
        new AuditEvent(
            workspaceId,
            actorUserId,
            action,
            targetType,
            targetId,
            outcome,
            requestId,
            metadata));
  }
}
