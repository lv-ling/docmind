package com.docmind.api.extraction.ai;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiCircuitBreaker {

  private enum State { CLOSED, OPEN, HALF_OPEN }

  private final int failureThreshold;
  private final java.time.Duration openDuration;
  private final Clock clock;
  private State state = State.CLOSED;
  private int consecutiveFailures;
  private Instant reopenAt;
  private boolean halfOpenProbeInFlight;

  @Autowired
  public AiCircuitBreaker(AiServiceProperties properties) {
    this(properties.circuitFailureThreshold(), properties.circuitOpenDuration(), Clock.systemUTC());
  }

  AiCircuitBreaker(int failureThreshold, java.time.Duration openDuration, Clock clock) {
    this.failureThreshold = failureThreshold;
    this.openDuration = openDuration;
    this.clock = clock;
  }

  public synchronized void acquirePermission() {
    if (state == State.OPEN && !clock.instant().isBefore(reopenAt)) {
      state = State.HALF_OPEN;
      halfOpenProbeInFlight = false;
    }
    if (state == State.OPEN || (state == State.HALF_OPEN && halfOpenProbeInFlight)) {
      throw new AiServiceClientException("AI_CIRCUIT_OPEN", true, null);
    }
    if (state == State.HALF_OPEN) {
      halfOpenProbeInFlight = true;
    }
  }

  public synchronized void recordSuccess() {
    state = State.CLOSED;
    consecutiveFailures = 0;
    reopenAt = null;
    halfOpenProbeInFlight = false;
  }

  public synchronized void recordFailure(boolean retryable) {
    if (!retryable) {
      if (state == State.HALF_OPEN) {
        state = State.CLOSED;
        consecutiveFailures = 0;
        halfOpenProbeInFlight = false;
      }
      return;
    }
    consecutiveFailures++;
    if (state == State.HALF_OPEN || consecutiveFailures >= failureThreshold) {
      state = State.OPEN;
      reopenAt = clock.instant().plus(openDuration);
      halfOpenProbeInFlight = false;
    }
  }

  synchronized String stateName() {
    return state.name();
  }
}
