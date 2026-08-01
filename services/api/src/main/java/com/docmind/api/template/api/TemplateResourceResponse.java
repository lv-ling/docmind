package com.docmind.api.template.api;

import com.docmind.api.template.domain.DocumentTemplateResource;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TemplateResourceResponse(
    UUID id,
    String kind,
    String contentType,
    long byteSize,
    String sha256,
    String downloadUrl) {
  public static TemplateResourceResponse from(DocumentTemplateResource resource) {
    return new TemplateResourceResponse(
        resource.id(),
        resource.kind().wireValue(),
        resource.contentType(),
        resource.byteSize(),
        resource.sha256(),
        "/api/v1/template-resources/" + resource.id() + "/content");
  }
}
