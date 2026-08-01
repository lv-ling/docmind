package com.docmind.api.extraction.ai;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "docmind.ai")
public record AiServiceProperties(
    boolean enabled,
    @NotBlank String baseUrl,
    @NotBlank String internalToken,
    @NotNull Duration connectTimeout,
    @NotNull Duration readTimeout,
    @Min(1) @Max(100) int circuitFailureThreshold,
    @NotNull Duration circuitOpenDuration) {

  @AssertTrue(message = "AI service settings contain an invalid URL, timeout or token")
  public boolean isValid() {
    try {
      URI uri = URI.create(baseUrl);
      return uri.isAbsolute()
          && ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
          && internalToken.length() >= 32
          && !connectTimeout.isZero()
          && !connectTimeout.isNegative()
          && !readTimeout.isZero()
          && !readTimeout.isNegative()
          && !circuitOpenDuration.isZero()
          && !circuitOpenDuration.isNegative();
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  @Override
  public String toString() {
    return "AiServiceProperties[enabled="
        + enabled
        + ", baseUrl="
        + baseUrl
        + ", internalToken=[REDACTED], connectTimeout="
        + connectTimeout
        + ", readTimeout="
        + readTimeout
        + ", circuitFailureThreshold="
        + circuitFailureThreshold
        + ", circuitOpenDuration="
        + circuitOpenDuration
        + "]";
  }
}
