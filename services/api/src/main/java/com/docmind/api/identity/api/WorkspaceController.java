package com.docmind.api.identity.api;

import com.docmind.api.identity.application.WorkspaceService;
import com.docmind.api.identity.application.WorkspaceService.CreateWorkspaceResult;
import com.docmind.api.identity.security.CurrentUser;
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
public class WorkspaceController {

  private final WorkspaceService workspaceService;
  private final CurrentUser currentUser;

  public WorkspaceController(WorkspaceService workspaceService, CurrentUser currentUser) {
    this.workspaceService = workspaceService;
    this.currentUser = currentUser;
  }

  @GetMapping("/workspaces")
  public List<WorkspaceSummaryResponse> listWorkspaces() {
    return workspaceService.listForUser(currentUser.id());
  }

  @PostMapping("/workspaces")
  public ResponseEntity<WorkspaceSummaryResponse> createWorkspace(
      @Valid @RequestBody CreateWorkspaceRequest request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      HttpServletRequest servletRequest) {
    CreateWorkspaceResult result =
        workspaceService.create(
            currentUser.id(),
            request,
            idempotencyKey,
            UUID.fromString(RequestContext.requestId(servletRequest)));
    HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
    return ResponseEntity.status(status).body(result.workspace());
  }

  @GetMapping("/workspaces/{workspaceId}/members")
  public List<WorkspaceMemberResponse> listMembers(@PathVariable UUID workspaceId) {
    return workspaceService.listMembers(currentUser.id(), workspaceId);
  }
}
