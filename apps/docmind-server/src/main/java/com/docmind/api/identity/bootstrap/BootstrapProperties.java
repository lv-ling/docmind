package com.docmind.api.identity.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docmind.bootstrap")
public record BootstrapProperties(
    boolean enabled,
    String email,
    String password,
    String displayName,
    String workspaceName,
    String workspaceSlug) {

  @Override
  public String toString() {
    return "BootstrapProperties[enabled=" + enabled + ", credentials=[REDACTED]]";
  }
}
