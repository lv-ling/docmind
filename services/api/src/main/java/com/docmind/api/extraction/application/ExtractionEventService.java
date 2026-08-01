package com.docmind.api.extraction.application;

import com.docmind.api.extraction.domain.ExtractionRun;
import com.docmind.api.extraction.domain.ExtractionRunStatus;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ExtractionEventService {

  private static final long EMITTER_TIMEOUT_MILLIS = Duration.ofMinutes(15).toMillis();
  private final Map<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

  public SseEmitter subscribe(ExtractionRun run) {
    SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
    emitters.computeIfAbsent(run.id(), ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
    emitter.onCompletion(() -> remove(run.id(), emitter));
    emitter.onTimeout(() -> remove(run.id(), emitter));
    emitter.onError(ignored -> remove(run.id(), emitter));
    send(emitter, snapshot(run));
    if (isTerminalForProcessing(run.status())) {
      emitter.complete();
    }
    return emitter;
  }

  public void publishAfterCommit(ExtractionRun run) {
    StatusSnapshot snapshot = snapshot(run);
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              publish(snapshot);
            }
          });
      return;
    }
    publish(snapshot);
  }

  @Scheduled(fixedDelayString = "${docmind.extraction.sse-heartbeat-millis:15000}")
  public void heartbeat() {
    emitters.forEach(
        (extractionId, current) ->
            current.forEach(
                emitter -> {
                  try {
                    emitter.send(SseEmitter.event().comment("keepalive"));
                  } catch (IOException | IllegalStateException exception) {
                    remove(extractionId, emitter);
                  }
                }));
  }

  private void publish(StatusSnapshot snapshot) {
    Set<SseEmitter> current = emitters.getOrDefault(snapshot.extractionId(), Set.of());
    current.forEach(emitter -> send(emitter, snapshot));
    if (isTerminalForProcessing(snapshot.status())) {
      current.forEach(SseEmitter::complete);
      emitters.remove(snapshot.extractionId());
    }
  }

  private void send(SseEmitter emitter, StatusSnapshot snapshot) {
    try {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("extraction_id", snapshot.extractionId());
      data.put("status", snapshot.status().wireValue());
      data.put("failure_code", snapshot.failureCode());
      emitter.send(
          SseEmitter.event()
              .name("status")
              .id(UUID.randomUUID().toString())
              .reconnectTime(2000)
              .data(data));
    } catch (IOException | IllegalStateException exception) {
      emitter.completeWithError(exception);
    }
  }

  private void remove(UUID extractionId, SseEmitter emitter) {
    Set<SseEmitter> current = emitters.get(extractionId);
    if (current == null) return;
    current.remove(emitter);
    if (current.isEmpty()) emitters.remove(extractionId, current);
  }

  private StatusSnapshot snapshot(ExtractionRun run) {
    return new StatusSnapshot(run.id(), run.status(), run.failureCode());
  }

  private boolean isTerminalForProcessing(ExtractionRunStatus status) {
    return status == ExtractionRunStatus.REVIEW_REQUIRED
        || status == ExtractionRunStatus.APPROVED
        || status == ExtractionRunStatus.FAILED;
  }

  private record StatusSnapshot(
      UUID extractionId, ExtractionRunStatus status, String failureCode) {}
}
