package com.docmind.api.extraction.api;

import com.docmind.api.extraction.application.ExtractionService;
import com.docmind.api.extraction.application.ExtractionReviewService;
import com.docmind.api.identity.security.CurrentUser;
import com.docmind.api.shared.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Validated
@RestController
@RequestMapping("/api/v1")
public class ExtractionController {

  private final ExtractionService extractionService;
  private final ExtractionReviewService reviewService;
  private final CurrentUser currentUser;

  public ExtractionController(
      ExtractionService extractionService,
      ExtractionReviewService reviewService,
      CurrentUser currentUser) {
    this.extractionService = extractionService;
    this.reviewService = reviewService;
    this.currentUser = currentUser;
  }

  @PostMapping("/source-versions/{sourceVersionId}/extractions")
  public ResponseEntity<AcceptedExtractionJobResponse> create(
      @PathVariable UUID sourceVersionId,
      @Valid @RequestBody CreateExtractionRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    UUID requestId = UUID.fromString(RequestContext.requestId(servletRequest));
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(
            extractionService.create(
                currentUser.id(), sourceVersionId, request, idempotencyKey, requestId));
  }

  @GetMapping("/extractions/{extractionId}")
  public ExtractionRunResponse get(@PathVariable UUID extractionId) {
    return reviewService.get(currentUser.id(), extractionId);
  }

  @GetMapping(value = "/extractions/{extractionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter events(@PathVariable UUID extractionId) {
    return reviewService.subscribe(currentUser.id(), extractionId);
  }

  @PatchMapping("/extractions/{extractionId}/fields/{fieldResultId}")
  public ExtractionRunResponse reviewField(
      @PathVariable UUID extractionId,
      @PathVariable UUID fieldResultId,
      @Valid @RequestBody ReviewExtractionFieldRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    return reviewService.reviewField(
        currentUser.id(),
        extractionId,
        fieldResultId,
        request,
        idempotencyKey,
        UUID.fromString(RequestContext.requestId(servletRequest)));
  }

  @PostMapping("/extractions/{extractionId}/approve")
  public ExtractionRunResponse approve(
      @PathVariable UUID extractionId,
      @Valid @RequestBody ApproveExtractionRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    return reviewService.approve(
        currentUser.id(),
        extractionId,
        request,
        idempotencyKey,
        UUID.fromString(RequestContext.requestId(servletRequest)));
  }
}
