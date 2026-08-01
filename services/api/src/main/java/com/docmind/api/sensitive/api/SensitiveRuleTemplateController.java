package com.docmind.api.sensitive.api;

import com.docmind.api.identity.security.CurrentUser;
import com.docmind.api.sensitive.application.SensitiveRuleTemplateService;
import com.docmind.api.sensitive.application.SensitiveRuleTemplateService.CreateResult;
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
public class SensitiveRuleTemplateController {

  private final SensitiveRuleTemplateService service;
  private final CurrentUser currentUser;

  public SensitiveRuleTemplateController(
      SensitiveRuleTemplateService service, CurrentUser currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @GetMapping("/workspaces/{workspaceId}/sensitive-rule-templates")
  public List<SensitiveRuleTemplateResponse> list(@PathVariable UUID workspaceId) {
    return service.list(currentUser.id(), workspaceId);
  }

  @PostMapping("/workspaces/{workspaceId}/sensitive-rule-templates")
  public ResponseEntity<SensitiveRuleTemplateDetailResponse> create(
      @PathVariable UUID workspaceId,
      @Valid @RequestBody CreateSensitiveRuleTemplateRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    return response(
        service.create(
            currentUser.id(),
            workspaceId,
            request,
            idempotencyKey,
            requestId(servletRequest)));
  }

  @GetMapping("/sensitive-rule-templates/{templateId}")
  public SensitiveRuleTemplateDetailResponse get(@PathVariable UUID templateId) {
    return service.get(currentUser.id(), templateId);
  }

  @PostMapping("/sensitive-rule-templates/{templateId}/versions")
  public ResponseEntity<SensitiveRuleTemplateVersionResponse> createVersion(
      @PathVariable UUID templateId,
      @Valid @RequestBody CreateSensitiveRuleTemplateVersionRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    return response(
        service.createVersion(
            currentUser.id(),
            templateId,
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
