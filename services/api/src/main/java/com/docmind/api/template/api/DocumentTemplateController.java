package com.docmind.api.template.api;

import com.docmind.api.identity.security.CurrentUser;
import com.docmind.api.shared.web.RequestContext;
import com.docmind.api.template.application.DocumentTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
public class DocumentTemplateController {
  private final DocumentTemplateService service;
  private final CurrentUser currentUser;

  public DocumentTemplateController(DocumentTemplateService service, CurrentUser currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @GetMapping("/workspaces/{workspaceId}/templates")
  public List<DocumentTemplateResponse> list(@PathVariable UUID workspaceId) {
    return service.list(currentUser.id(), workspaceId);
  }

  @PostMapping("/source-versions/{sourceVersionId}/templates")
  public ResponseEntity<AcceptedTemplateJobResponse> create(
      @PathVariable UUID sourceVersionId,
      @Valid @RequestBody CreateTemplateRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(
            service.create(
                currentUser.id(),
                sourceVersionId,
                request,
                idempotencyKey,
                UUID.fromString(RequestContext.requestId(servletRequest))));
  }

  @GetMapping("/templates/{templateId}")
  public DocumentTemplateDetailResponse get(@PathVariable UUID templateId) {
    return service.get(currentUser.id(), templateId);
  }

  @PostMapping("/templates/{templateId}/versions")
  public DocumentTemplateVersionResponse createVersion(
      @PathVariable UUID templateId,
      @Valid @RequestBody CreateTemplateVersionRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    return service.createVersion(
        currentUser.id(),
        templateId,
        request,
        idempotencyKey,
        UUID.fromString(RequestContext.requestId(servletRequest)));
  }

  @PostMapping("/templates/{templateId}/versions/{versionId}/publish")
  public DocumentTemplateVersionResponse publish(
      @PathVariable UUID templateId,
      @PathVariable UUID versionId,
      @Valid @RequestBody PublishTemplateVersionRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    return service.publish(
        currentUser.id(),
        templateId,
        versionId,
        request,
        idempotencyKey,
        UUID.fromString(RequestContext.requestId(servletRequest)));
  }

  @PostMapping("/templates/{templateId}/rollback")
  public DocumentTemplateVersionResponse rollback(
      @PathVariable UUID templateId,
      @Valid @RequestBody RollbackTemplateRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    return service.rollback(
        currentUser.id(),
        templateId,
        request,
        idempotencyKey,
        UUID.fromString(RequestContext.requestId(servletRequest)));
  }

  @GetMapping("/template-resources/{resourceId}/content")
  public ResponseEntity<byte[]> resource(@PathVariable UUID resourceId) {
    var resource = service.getResource(currentUser.id(), resourceId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(resource.contentType()));
    headers.setContentLength(resource.bytes().length);
    headers.setContentDisposition(
        ContentDisposition.inline()
            .filename(resource.filename(), StandardCharsets.UTF_8)
            .build());
    headers.setCacheControl(CacheControl.noStore());
    headers.set("X-Content-Type-Options", "nosniff");
    headers.set("Content-Security-Policy", "sandbox");
    return new ResponseEntity<>(resource.bytes(), headers, HttpStatus.OK);
  }
}
