package com.docmind.api.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 254)
  private String email;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected UserAccount() {}

  public UserAccount(String email, String displayName, String passwordHash) {
    this.email = email;
    this.displayName = displayName;
    this.passwordHash = passwordHash;
    this.status = UserStatus.ACTIVE;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public UUID id() {
    return id;
  }

  public String email() {
    return email;
  }

  public String displayName() {
    return displayName;
  }

  public String passwordHash() {
    return passwordHash;
  }

  public UserStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
