package com.docmind.api.extraction.messaging;

import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class RetryBackoffPolicy {

  private final AsyncJobProperties properties;

  public RetryBackoffPolicy(AsyncJobProperties properties) {
    this.properties = properties;
  }

  public Duration delayAfterAttempt(int attemptCount) {
    Duration delay = properties.retryInitialDelay();
    for (int index = 1; index < Math.max(1, attemptCount); index++) {
      long multiplied =
          (long) Math.ceil(delay.toMillis() * properties.retryMultiplier());
      if (multiplied >= properties.retryMaxDelay().toMillis()) {
        return properties.retryMaxDelay();
      }
      delay = Duration.ofMillis(multiplied);
    }
    return delay.compareTo(properties.retryMaxDelay()) > 0
        ? properties.retryMaxDelay()
        : delay;
  }
}
