package com.docmind.api.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "workspace_id", nullable = false, updatable = false)
  private UUID workspaceId;

  @Column(name = "actor_user_id", nullable = false, updatable = false)
  private UUID actorUserId;

  @Column(nullable = false, updatable = false, length = 100)
  private String action;

  @Column(name = "target_type", nullable = false, updatable = false, length = 80)
  private String targetType;

  @Column(name = "target_id", nullable = false, updatable = false)
  private UUID targetId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 20)
  private AuditOutcome outcome;

  @Column(name = "request_id", nullable = false, updatable = false)
  private UUID requestId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, updatable = false)
  private Map<String, Object> metadata;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  protected AuditEvent() {}

  public AuditEvent(
      UUID workspaceId,
      UUID actorUserId,
      String action,
      String targetType,
      UUID targetId,
      AuditOutcome outcome,
      UUID requestId,
      Map<String, Object> metadata) {
    this.workspaceId = workspaceId;
    this.actorUserId = actorUserId;
    this.action = action;
    this.targetType = targetType;
    this.targetId = targetId;
    this.outcome = outcome;
    this.requestId = requestId;
    this.metadata = Map.copyOf(metadata);
    this.occurredAt = Instant.now();
  }

  public UUID id() {
    return id;
  }

  public UUID workspaceId() {
    return workspaceId;
  }

  public String action() {
    return action;
  }

  public AuditOutcome outcome() {
    return outcome;
  }
}
