package com.docmind.api.infrastructure.storage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "docmind.storage")
public record DocmindStorageProperties(
    @NotBlank String endpoint,
    @NotBlank String accessKey,
    @NotBlank String secretKey,
    @NotNull Duration uploadUrlTtl,
    @Valid @NotNull Buckets buckets) {

  @AssertTrue(message = "upload-url-ttl must be between 60 seconds and 7 days")
  public boolean isUploadUrlTtlValid() {
    return uploadUrlTtl != null
        && uploadUrlTtl.compareTo(Duration.ofSeconds(60)) >= 0
        && uploadUrlTtl.compareTo(Duration.ofDays(7)) <= 0;
  }

  public record Buckets(
      @NotBlank String sources,
      @NotBlank String previews,
      @NotBlank String templates,
      @NotBlank String exports) {

    public List<String> all() {
      return List.of(sources, previews, templates, exports);
    }
  }

  @Override
  public String toString() {
    return "DocmindStorageProperties[endpoint="
        + endpoint
        + ", accessKey=[REDACTED], secretKey=[REDACTED], buckets="
        + buckets
        + ", uploadUrlTtl="
        + uploadUrlTtl
        + "]";
  }
}
