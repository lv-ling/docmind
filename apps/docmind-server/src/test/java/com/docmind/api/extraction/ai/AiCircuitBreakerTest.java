package com.docmind.api.extraction.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AiCircuitBreakerTest {

  @Test
  void opensAfterRetryableFailuresAndAllowsOneHalfOpenProbe() {
    MutableClock clock = new MutableClock(Instant.parse("2026-08-01T00:00:00Z"));
    AiCircuitBreaker breaker = new AiCircuitBreaker(2, Duration.ofSeconds(30), clock);

    breaker.acquirePermission();
    breaker.recordFailure(true);
    breaker.acquirePermission();
    breaker.recordFailure(true);

    assertThat(breaker.stateName()).isEqualTo("OPEN");
    assertThatThrownBy(breaker::acquirePermission)
        .isInstanceOf(AiServiceClientException.class)
        .extracting(exception -> ((AiServiceClientException) exception).failureCode())
        .isEqualTo("AI_CIRCUIT_OPEN");

    clock.advance(Duration.ofSeconds(30));
    breaker.acquirePermission();
    assertThat(breaker.stateName()).isEqualTo("HALF_OPEN");
    assertThatThrownBy(breaker::acquirePermission)
        .isInstanceOf(AiServiceClientException.class);
    breaker.recordSuccess();
    assertThat(breaker.stateName()).isEqualTo("CLOSED");
  }

  @Test
  void permanentClientErrorsDoNotOpenTheDependencyCircuit() {
    MutableClock clock = new MutableClock(Instant.parse("2026-08-01T00:00:00Z"));
    AiCircuitBreaker breaker = new AiCircuitBreaker(1, Duration.ofSeconds(30), clock);

    breaker.acquirePermission();
    breaker.recordFailure(false);

    assertThat(breaker.stateName()).isEqualTo("CLOSED");
    breaker.acquirePermission();
  }

  private static final class MutableClock extends Clock {
    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    private void advance(Duration duration) {
      instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
