package com.docmind.api.identity.bootstrap;

import com.docmind.api.identity.api.CreateWorkspaceRequest;
import com.docmind.api.identity.application.WorkspaceService;
import com.docmind.api.identity.domain.UserAccount;
import com.docmind.api.identity.infrastructure.UserAccountRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalBootstrapService {

  private final UserAccountRepository users;
  private final PasswordEncoder passwordEncoder;
  private final WorkspaceService workspaces;

  public LocalBootstrapService(
      UserAccountRepository users, PasswordEncoder passwordEncoder, WorkspaceService workspaces) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.workspaces = workspaces;
  }

  @Transactional
  public void initialize(BootstrapProperties properties) {
    validate(properties);
    String email = properties.email().strip().toLowerCase(Locale.ROOT);
    UserAccount user =
        users
            .findByEmail(email)
            .orElseGet(
                () ->
                    users.saveAndFlush(
                        new UserAccount(
                            email,
                            properties.displayName().strip(),
                            passwordEncoder.encode(properties.password()))));

    workspaces.create(
        user.id(),
        new CreateWorkspaceRequest(
            properties.workspaceName().strip(), properties.workspaceSlug().strip()),
        "local-bootstrap-v1",
        UUID.randomUUID());
  }

  private void validate(BootstrapProperties properties) {
    if (isBlank(properties.email())
        || isBlank(properties.password())
        || properties.password().length() < 8
        || isBlank(properties.displayName())
        || isBlank(properties.workspaceName())
        || isBlank(properties.workspaceSlug())) {
      throw new IllegalStateException("Local bootstrap configuration is incomplete");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
