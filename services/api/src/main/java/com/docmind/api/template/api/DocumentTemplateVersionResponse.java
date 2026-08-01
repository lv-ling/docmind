package com.docmind.api.template.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public record DocumentTemplateVersionResponse(
    UUID id,
    UUID templateId,
    UUID workspaceId,
    UUID sourceVersionId,
    UUID parsedContentId,
    int versionNumber,
    String status,
    SafeHtmlDocumentResponse document,
    JsonNode documentModel,
    List<TemplateResourceResponse> resources,
    List<ConversionWarningResponse> warnings,
    String changeSummary,
    JsonNode diff,
    Instant createdAt,
    UUID createdBy,
    Instant publishedAt) {}
