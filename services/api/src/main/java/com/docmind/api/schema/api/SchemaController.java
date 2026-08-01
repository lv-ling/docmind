package com.docmind.api.schema.api;

import com.docmind.api.identity.security.CurrentUser;
import com.docmind.api.schema.application.SchemaService;
import com.docmind.api.schema.application.SchemaService.CreateResult;
import com.docmind.api.shared.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class SchemaController {

  private final SchemaService schemaService;
  private final CurrentUser currentUser;

  public SchemaController(SchemaService schemaService, CurrentUser currentUser) {
    this.schemaService = schemaService;
    this.currentUser = currentUser;
  }

  @GetMapping("/workspaces/{workspaceId}/schemas")
  public List<ExtractionSchemaResponse> list(@PathVariable UUID workspaceId) {
    return schemaService.list(currentUser.id(), workspaceId);
  }

  @PostMapping("/workspaces/{workspaceId}/schemas")
  public ResponseEntity<ExtractionSchemaDetailResponse> create(
      @PathVariable UUID workspaceId,
      @Valid @RequestBody CreateSchemaRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    CreateResult<ExtractionSchemaDetailResponse> result =
        schemaService.create(
            currentUser.id(),
            workspaceId,
            request,
            idempotencyKey,
            requestId(servletRequest));
    return response(result);
  }

  @GetMapping("/schemas/{schemaId}")
  public ExtractionSchemaDetailResponse get(@PathVariable UUID schemaId) {
    return schemaService.get(currentUser.id(), schemaId);
  }

  @PostMapping("/schemas/{schemaId}/versions")
  public ResponseEntity<SchemaVersionResponse> createVersion(
      @PathVariable UUID schemaId,
      @Valid @RequestBody CreateSchemaVersionRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    return response(
        schemaService.createVersion(
            currentUser.id(), schemaId, request, idempotencyKey, requestId(servletRequest)));
  }

  @GetMapping("/workspaces/{workspaceId}/schema-templates")
  public List<SchemaTemplateResponse> listTemplates(@PathVariable UUID workspaceId) {
    return schemaService.listTemplates(currentUser.id(), workspaceId);
  }

  @PostMapping("/workspaces/{workspaceId}/schema-templates")
  public ResponseEntity<SchemaTemplateResponse> createTemplate(
      @PathVariable UUID workspaceId,
      @Valid @RequestBody CreateSchemaTemplateRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    return response(
        schemaService.createTemplate(
            currentUser.id(),
            workspaceId,
            request,
            idempotencyKey,
            requestId(servletRequest)));
  }

  private UUID requestId(HttpServletRequest request) {
    return UUID.fromString(RequestContext.requestId(request));
  }

  private <T> ResponseEntity<T> response(CreateResult<T> result) {
    return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
        .body(result.response());
  }
}
