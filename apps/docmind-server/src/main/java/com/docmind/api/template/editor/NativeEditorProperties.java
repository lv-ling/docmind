package com.docmind.api.template.editor;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "docmind.native-editor")
public record NativeEditorProperties(
    boolean enabled,
    String documentServerPublicUrl,
    String documentServerDownloadUrl,
    String applicationInternalUrl,
    String jwtSecret,
    String jwtHeader,
    @NotNull Duration sessionTtl,
    long maxFileSizeBytes,
    @NotNull List<String> callbackAllowedOrigins) {

  @AssertTrue(message = "native editor configuration is invalid")
  public boolean isValid() {
    if (!enabled) return true;
    return validHttpUrl(documentServerPublicUrl)
        && validHttpUrl(documentServerDownloadUrl)
        && validHttpUrl(applicationInternalUrl)
        && jwtSecret != null
        && jwtSecret.length() >= 32
        && jwtHeader != null
        && jwtHeader.matches("[A-Za-z0-9-]{1,64}")
        && sessionTtl != null
        && sessionTtl.compareTo(Duration.ofMinutes(5)) >= 0
        && sessionTtl.compareTo(Duration.ofHours(24)) <= 0
        && maxFileSizeBytes >= 1024
        && maxFileSizeBytes <= 50L * 1024L * 1024L
        && callbackAllowedOrigins != null
        && !callbackAllowedOrigins.isEmpty()
        && callbackAllowedOrigins.stream().allMatch(this::validOrigin);
  }

  public String normalizedDocumentServerPublicUrl() {
    return withoutTrailingSlash(documentServerPublicUrl);
  }

  public String normalizedDocumentServerDownloadUrl() {
    return withoutTrailingSlash(documentServerDownloadUrl);
  }

  public String normalizedApplicationInternalUrl() {
    return withoutTrailingSlash(applicationInternalUrl);
  }

  private boolean validOrigin(String value) {
    if (!validHttpUrl(value)) return false;
    URI uri = URI.create(value);
    return (uri.getPath() == null || uri.getPath().isBlank() || "/".equals(uri.getPath()))
        && uri.getQuery() == null
        && uri.getFragment() == null;
  }

  private boolean validHttpUrl(String value) {
    if (value == null || value.isBlank()) return false;
    try {
      URI uri = URI.create(value);
      return ("http".equalsIgnoreCase(uri.getScheme())
              || "https".equalsIgnoreCase(uri.getScheme()))
          && uri.getHost() != null
          && uri.getUserInfo() == null;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  private String withoutTrailingSlash(String value) {
    String normalized = value.strip();
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }
}
