package com.docmind.api.source.api;

public record CompleteSourceUploadResponse(
    SourceDocumentResponse source, SourceVersionResponse version) {}
