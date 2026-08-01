package com.docmind.api.extraction.messaging;

import com.docmind.api.extraction.domain.AsyncJobType;

public interface AsyncJobHandler {

  AsyncJobType jobType();

  void handle(AsyncJobCommand command);

  default void onRetryScheduled(AsyncJobCommand command, String failureCode) {}

  default void onTerminalFailure(AsyncJobCommand command, String failureCode) {}
}
