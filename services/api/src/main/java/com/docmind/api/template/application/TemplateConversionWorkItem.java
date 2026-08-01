package com.docmind.api.template.application;

import com.docmind.api.source.domain.SourcePreview;
import com.docmind.api.source.domain.SourceVersion;
import com.docmind.api.template.domain.DocumentTemplate;

public record TemplateConversionWorkItem(
    DocumentTemplate template, SourceVersion source, SourcePreview preview) {}
