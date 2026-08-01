package com.docmind.api.source.api;

import java.util.List;

public record SourceDocumentDetailResponse(
    SourceDocumentResponse source, List<SourceVersionResponse> versions) {}
