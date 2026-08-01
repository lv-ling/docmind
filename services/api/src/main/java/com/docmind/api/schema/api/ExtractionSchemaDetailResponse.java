package com.docmind.api.schema.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ExtractionSchemaDetailResponse(
    ExtractionSchemaResponse schema,
    SchemaVersionResponse currentVersion,
    List<SchemaVersionResponse> versions) {}
