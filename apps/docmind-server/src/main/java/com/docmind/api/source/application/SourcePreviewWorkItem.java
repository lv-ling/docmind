package com.docmind.api.source.application;

import com.docmind.api.source.domain.SourcePreview;
import com.docmind.api.source.domain.SourceVersion;

public record SourcePreviewWorkItem(SourcePreview preview, SourceVersion source) {}
