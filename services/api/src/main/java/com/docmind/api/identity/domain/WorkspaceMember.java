package com.docmind.api.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "workspace_member",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_workspace_member",
          columnNames = {"workspace_id", "user_id"})
    })
public class WorkspaceMember {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workspace_id", nullable = false)
  private Workspace workspace;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private WorkspaceRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MemberStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  protected WorkspaceMember() {}

  public WorkspaceMember(Workspace workspace, UserAccount user, WorkspaceRole role) {
    this.workspace = workspace;
    this.user = user;
    this.role = role;
    this.status = MemberStatus.ACTIVE;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public UUID id() {
    return id;
  }

  public Workspace workspace() {
    return workspace;
  }

  public UserAccount user() {
    return user;
  }

  public WorkspaceRole role() {
    return role;
  }

  public MemberStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
