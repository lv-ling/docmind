package com.docmind.api.extraction.messaging;

public enum JobClaimDecision {
  CLAIMED,
  ACKNOWLEDGE_DUPLICATE,
  REQUEUE_NOT_DUE,
  REJECT_INVALID
}
