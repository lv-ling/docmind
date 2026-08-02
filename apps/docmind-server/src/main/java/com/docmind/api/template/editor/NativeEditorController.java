package com.docmind.api.template.editor;

import com.docmind.api.identity.security.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(prefix = "docmind.native-editor", name = "enabled", havingValue = "true")
public class NativeEditorController {

  private final NativeEditorSessionService service;
  private final CurrentUser currentUser;
  private final NativeEditorProperties properties;

  public NativeEditorController(
      NativeEditorSessionService service,
      CurrentUser currentUser,
      NativeEditorProperties properties) {
    this.service = service;
    this.currentUser = currentUser;
    this.properties = properties;
  }

  @PostMapping("/templates/{templateId}/editor-sessions")
  public ResponseEntity<NativeEditorSessionResponse> create(@PathVariable UUID templateId) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.create(currentUser.id(), templateId));
  }

  @GetMapping("/template-editor-sessions/{sessionId}")
  public NativeEditorSessionStatusResponse status(@PathVariable UUID sessionId) {
    return service.status(currentUser.id(), sessionId);
  }

  @GetMapping("/template-editor-sessions/{sessionId}/content")
  public ResponseEntity<byte[]> content(
      @PathVariable UUID sessionId,
      @RequestParam(name = "access_token") String accessToken) {
    NativeEditorBinaryContent content = service.content(sessionId, accessToken);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(content.mimeType()));
    headers.setContentLength(content.bytes().length);
    headers.setContentDisposition(
        ContentDisposition.attachment()
            .filename(content.fileName(), StandardCharsets.UTF_8)
            .build());
    headers.setCacheControl(CacheControl.noStore());
    headers.set("X-Content-Type-Options", "nosniff");
    return new ResponseEntity<>(content.bytes(), headers, HttpStatus.OK);
  }

  @PostMapping("/integrations/onlyoffice/callback/{sessionId}")
  public Map<String, Integer> callback(
      @PathVariable UUID sessionId,
      @RequestParam(name = "access_token") String accessToken,
      HttpServletRequest request,
      @RequestBody JsonNode body) {
    service.handleCallback(
        sessionId, accessToken, request.getHeader(properties.jwtHeader()), body);
    return Map.of("error", 0);
  }
}
