package com.docmind.api.identity.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "docmind.auth")
public record AuthProperties(
    @NotBlank String issuer,
    @NotBlank String audience,
    @NotNull Duration accessTokenTtl,
    @NotBlank @Size(min = 32) String secret) {

  @Override
  public String toString() {
    return "AuthProperties[issuer="
        + issuer
        + ", audience="
        + audience
        + ", accessTokenTtl="
        + accessTokenTtl
        + ", secret=[REDACTED]]";
  }
}
