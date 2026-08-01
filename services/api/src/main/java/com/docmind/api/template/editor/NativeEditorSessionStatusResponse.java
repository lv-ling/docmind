package com.docmind.api.template.editor;

import java.time.Instant;
import java.util.UUID;

public record NativeEditorSessionStatusResponse(
    UUID sessionId,
    UUID templateId,
    String status,
    Instant expiresAt,
    Integer lastCallbackStatus,
    int callbackCount,
    String savedSha256,
    Long savedSizeBytes,
    Instant savedAt) {}
