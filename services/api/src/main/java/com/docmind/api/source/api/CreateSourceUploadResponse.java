package com.docmind.api.source.api;

public record CreateSourceUploadResponse(
    SourceDocumentResponse source,
    SourceVersionResponse version,
    UploadSessionResponse upload) {}
