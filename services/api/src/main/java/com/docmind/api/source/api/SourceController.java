package com.docmind.api.source.api;

import com.docmind.api.identity.security.CurrentUser;
import com.docmind.api.shared.web.RequestContext;
import com.docmind.api.source.application.SourceService;
import com.docmind.api.source.application.SourceService.BinaryContent;
import com.docmind.api.source.application.SourceService.CreateUploadResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class SourceController {

  private final SourceService sourceService;
  private final CurrentUser currentUser;

  public SourceController(SourceService sourceService, CurrentUser currentUser) {
    this.sourceService = sourceService;
    this.currentUser = currentUser;
  }

  @GetMapping("/workspaces/{workspaceId}/sources")
  public SourceDocumentPageResponse listSources(
      @PathVariable UUID workspaceId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Integer limit) {
    return sourceService.listSources(currentUser.id(), workspaceId, cursor, limit);
  }

  @PostMapping("/workspaces/{workspaceId}/sources")
  public ResponseEntity<CreateSourceUploadResponse> createSourceUpload(
      @PathVariable UUID workspaceId,
      @Valid @RequestBody CreateSourceUploadRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    CreateUploadResult result =
        sourceService.createSourceUpload(
            currentUser.id(),
            workspaceId,
            request,
            idempotencyKey,
            UUID.fromString(RequestContext.requestId(servletRequest)));
    return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
        .body(result.response());
  }

  @GetMapping("/sources/{sourceId}")
  public SourceDocumentDetailResponse getSource(@PathVariable UUID sourceId) {
    return sourceService.getSource(currentUser.id(), sourceId);
  }

  @PostMapping("/sources/{sourceId}/versions")
  public ResponseEntity<CreateSourceUploadResponse> createVersionUpload(
      @PathVariable UUID sourceId,
      @Valid @RequestBody CreateSourceVersionUploadRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    CreateUploadResult result =
        sourceService.createVersionUpload(
            currentUser.id(),
            sourceId,
            request,
            idempotencyKey,
            UUID.fromString(RequestContext.requestId(servletRequest)));
    return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
        .body(result.response());
  }

  @PostMapping("/source-versions/{sourceVersionId}/complete")
  public CompleteSourceUploadResponse completeSourceUpload(
      @PathVariable UUID sourceVersionId,
      @Valid @RequestBody CompleteSourceUploadRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    return sourceService.completeUpload(
        currentUser.id(),
        sourceVersionId,
        request,
        idempotencyKey,
        UUID.fromString(RequestContext.requestId(servletRequest)));
  }

  @GetMapping("/source-versions/{sourceVersionId}/preview")
  public SourcePreviewAccessResponse getPreview(@PathVariable UUID sourceVersionId) {
    return sourceService.getPreview(currentUser.id(), sourceVersionId);
  }

  @GetMapping("/source-versions/{sourceVersionId}/content")
  public ResponseEntity<byte[]> getOriginalContent(@PathVariable UUID sourceVersionId) {
    return binaryResponse(sourceService.getOriginalContent(currentUser.id(), sourceVersionId));
  }

  @GetMapping("/source-previews/{sourcePreviewId}/content")
  public ResponseEntity<byte[]> getPreviewContent(@PathVariable UUID sourcePreviewId) {
    return binaryResponse(sourceService.getPreviewContent(currentUser.id(), sourcePreviewId));
  }

  private ResponseEntity<byte[]> binaryResponse(BinaryContent content) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(content.mimeType()));
    headers.setContentLength(content.bytes().length);
    headers.setContentDisposition(
        ContentDisposition.inline()
            .filename(content.fileName(), StandardCharsets.UTF_8)
            .build());
    headers.setCacheControl(CacheControl.noStore());
    headers.set("X-Content-Type-Options", "nosniff");
    headers.set("Content-Security-Policy", "sandbox");
    if (content.etag() != null && !content.etag().isBlank()) {
      headers.setETag("\"" + content.etag().replace("\"", "") + "\"");
    }
    return new ResponseEntity<>(content.bytes(), headers, HttpStatus.OK);
  }
}
