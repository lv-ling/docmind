package com.docmind.api.extraction.messaging;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "docmind.jobs")
public record AsyncJobProperties(
    boolean enabled,
    boolean dispatcherEnabled,
    boolean consumerEnabled,
    @NotBlank String exchange,
    @NotBlank String routingKey,
    @NotBlank String queue,
    @NotBlank String deadLetterExchange,
    @NotBlank String deadLetterRoutingKey,
    @NotBlank String deadLetterQueue,
    @NotNull Duration dispatchInitialDelay,
    @NotNull Duration dispatchInterval,
    @NotNull Duration publishConfirmTimeout,
    @NotNull Duration publishLease,
    @NotNull Duration publishRetryDelay,
    @NotNull Duration workerLease,
    @NotNull Duration recoveryInterval,
    @NotNull Duration retryInitialDelay,
    @NotNull Duration retryMaxDelay,
    @DecimalMin("1.0") double retryMultiplier,
    @Min(1) @Max(100) int dispatchBatchSize,
    @Min(1) @Max(100) int consumerPrefetch) {

  @AssertTrue(message = "all job durations must be positive and retry bounds must be ordered")
  public boolean areDurationsValid() {
    return positive(dispatchInitialDelay)
        && positive(dispatchInterval)
        && positive(publishConfirmTimeout)
        && positive(publishLease)
        && positive(publishRetryDelay)
        && positive(workerLease)
        && positive(recoveryInterval)
        && positive(retryInitialDelay)
        && positive(retryMaxDelay)
        && retryInitialDelay.compareTo(retryMaxDelay) <= 0;
  }

  private boolean positive(Duration duration) {
    return duration != null && !duration.isZero() && !duration.isNegative();
  }
}
