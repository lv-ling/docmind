package com.docmind.api.extraction.messaging;

import com.docmind.api.extraction.domain.AsyncJobType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AsyncJobHandlerRegistry {

  private final Map<AsyncJobType, AsyncJobHandler> handlers;

  public AsyncJobHandlerRegistry(List<AsyncJobHandler> handlers) {
    EnumMap<AsyncJobType, AsyncJobHandler> registered = new EnumMap<>(AsyncJobType.class);
    for (AsyncJobHandler handler : handlers) {
      AsyncJobHandler previous = registered.put(handler.jobType(), handler);
      if (previous != null) {
        throw new IllegalStateException(
            "Multiple async job handlers registered for " + handler.jobType());
      }
    }
    this.handlers = Map.copyOf(registered);
  }

  public boolean supports(AsyncJobType jobType) {
    return handlers.containsKey(jobType);
  }

  public Optional<AsyncJobHandler> find(AsyncJobType jobType) {
    return Optional.ofNullable(handlers.get(jobType));
  }
}
