package com.docmind.api.template.editor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NativeEditorSessionResponse(
    UUID sessionId,
    String editorUrl,
    Map<String, Object> editorConfig,
    Instant expiresAt) {}
