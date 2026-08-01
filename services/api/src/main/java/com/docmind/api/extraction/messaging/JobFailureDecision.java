package com.docmind.api.extraction.messaging;

public enum JobFailureDecision {
  RETRY_SCHEDULED,
  TERMINAL_FAILURE,
  ALREADY_FINALIZED
}
