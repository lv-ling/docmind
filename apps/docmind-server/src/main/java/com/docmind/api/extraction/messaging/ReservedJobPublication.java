package com.docmind.api.extraction.messaging;

import java.util.UUID;

public record ReservedJobPublication(
    UUID jobId, UUID publishLeaseId, AsyncJobCommand command) {}
