package com.docmind.api.template.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AcceptedTemplateJobResponse(UUID jobId, UUID templateId, UUID requestId) {}
