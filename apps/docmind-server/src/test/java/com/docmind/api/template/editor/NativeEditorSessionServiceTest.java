package com.docmind.api.template.editor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.docmind.api.identity.application.WorkspaceAccessService;
import com.docmind.api.identity.application.WorkspacePermission;
import com.docmind.api.identity.domain.UserAccount;
import com.docmind.api.identity.domain.WorkspaceMember;
import com.docmind.api.infrastructure.storage.DocmindStorageProperties;
import com.docmind.api.infrastructure.storage.ObjectStorage;
import com.docmind.api.source.application.SourceService;
import com.docmind.api.source.application.SourceService.BinaryContent;
import com.docmind.api.template.domain.DocumentTemplate;
import com.docmind.api.template.infrastructure.DocumentTemplateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class NativeEditorSessionServiceTest {

  private static final String DOCX_MIME =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final String SECRET =
      "test-onlyoffice-jwt-secret-with-at-least-32-characters";

  @Test
  void createsSignedConfigAndProtectsDocumentWithOpaqueSessionToken() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID workspaceId = UUID.randomUUID();
    UUID templateId = UUID.randomUUID();
    UUID sourceVersionId = UUID.randomUUID();
    Instant now = Instant.parse("2026-08-01T10:00:00Z");
    NativeEditorProperties properties = properties();
    ObjectMapper objectMapper = new ObjectMapper();
    OnlyOfficeJwtService jwtService = new OnlyOfficeJwtService(objectMapper, properties);

    DocumentTemplate template =
        new DocumentTemplate(
            templateId,
            workspaceId,
            UUID.randomUUID(),
            sourceVersionId,
            UUID.randomUUID(),
            "合同模板",
            userId,
            "idem",
            "hash",
            now);
    template.complete(UUID.randomUUID(), userId, now);

    DocumentTemplateRepository templates = mock(DocumentTemplateRepository.class);
    WorkspaceAccessService access = mock(WorkspaceAccessService.class);
    SourceService sourceService = mock(SourceService.class);
    ObjectStorage storage = mock(ObjectStorage.class);
    WorkspaceMember membership = mock(WorkspaceMember.class);
    UserAccount user = mock(UserAccount.class);
    byte[] originalBytes = minimalDocx();
    when(templates.findById(templateId)).thenReturn(Optional.of(template));
    when(access.require(userId, workspaceId, WorkspacePermission.EDIT_CONTENT))
        .thenReturn(membership);
    when(membership.user()).thenReturn(user);
    when(user.displayName()).thenReturn("本地管理员");
    when(sourceService.getOriginalContent(userId, sourceVersionId))
        .thenReturn(
            new BinaryContent(originalBytes, "application/octet-stream", "两页合同.docx", "etag"));

    NativeEditorSessionService service =
        new NativeEditorSessionService(
            templates,
            access,
            sourceService,
            storage,
            storageProperties(),
            properties,
            jwtService,
            Clock.fixed(now, ZoneOffset.UTC));

    NativeEditorSessionResponse response = service.create(userId, templateId);

    assertThat(response.editorUrl()).isEqualTo("http://127.0.0.1:8082");
    assertThat(response.expiresAt()).isEqualTo(now.plus(Duration.ofHours(2)));
    String token = (String) response.editorConfig().get("token");
    JsonNode signed = jwtService.verify(token);
    assertThat(signed.path("document").path("key").asText()).startsWith("docmind-");
    assertThat(signed.path("document").path("permissions").path("modifyContentControl").asBoolean())
        .isTrue();
    assertThat(signed.path("editorConfig").path("callbackUrl").asText())
        .startsWith("http://host.docker.internal:8080/api/v1/integrations/onlyoffice/callback/");

    @SuppressWarnings("unchecked")
    Map<String, Object> document = (Map<String, Object>) response.editorConfig().get("document");
    URI documentUri = URI.create((String) document.get("url"));
    String accessToken = documentUri.getQuery().substring("access_token=".length());
    NativeEditorBinaryContent content = service.content(response.sessionId(), accessToken);
    assertThat(content.fileName()).isEqualTo("两页合同.docx");
    assertThat(content.bytes()).isEqualTo(originalBytes);
    assertThatThrownBy(() -> service.content(response.sessionId(), "wrong-token"))
        .hasMessage("原生编辑会话不存在");

    String documentKey = (String) document.get("key");
    JsonNode editingCallback =
        objectMapper.valueToTree(Map.of("key", documentKey, "status", 1));
    String callbackJwt =
        jwtService.sign(Map.of("payload", Map.of("key", documentKey, "status", 1)));
    service.handleCallback(
        response.sessionId(), accessToken, "Bearer " + callbackJwt, editingCallback);
    NativeEditorSessionStatusResponse status = service.status(userId, response.sessionId());
    assertThat(status.status()).isEqualTo("editing");
    assertThat(status.callbackCount()).isEqualTo(1);

    JsonNode tamperedCallback =
        objectMapper.valueToTree(Map.of("key", documentKey, "status", 4));
    assertThatThrownBy(
            () ->
                service.handleCallback(
                    response.sessionId(), accessToken, "Bearer " + callbackJwt, tamperedCallback))
        .hasMessage("编辑服务回调校验失败");
  }

  @Test
  void rejectsTamperedOnlyOfficeJwt() {
    OnlyOfficeJwtService jwtService = new OnlyOfficeJwtService(new ObjectMapper(), properties());
    String token = jwtService.sign(Map.of("documentType", "word"));
    String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

    assertThatThrownBy(() -> jwtService.verify(tampered))
        .isInstanceOf(SecurityException.class)
        .hasMessage("Invalid callback JWT signature");
  }

  @Test
  void reloadsLatestPocArtifactFromObjectStorageAfterServiceRestart() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID workspaceId = UUID.randomUUID();
    UUID templateId = UUID.randomUUID();
    UUID sourceVersionId = UUID.randomUUID();
    Instant now = Instant.parse("2026-08-01T10:00:00Z");
    NativeEditorProperties properties = properties();
    OnlyOfficeJwtService jwtService =
        new OnlyOfficeJwtService(new ObjectMapper(), properties);

    DocumentTemplate template =
        new DocumentTemplate(
            templateId,
            workspaceId,
            UUID.randomUUID(),
            sourceVersionId,
            UUID.randomUUID(),
            "合同模板",
            userId,
            "idem",
            "hash",
            now);
    template.complete(UUID.randomUUID(), userId, now);

    DocumentTemplateRepository templates = mock(DocumentTemplateRepository.class);
    WorkspaceAccessService access = mock(WorkspaceAccessService.class);
    SourceService sourceService = mock(SourceService.class);
    ObjectStorage storage = mock(ObjectStorage.class);
    WorkspaceMember membership = mock(WorkspaceMember.class);
    UserAccount user = mock(UserAccount.class);
    byte[] originalBytes = minimalDocx("original");
    byte[] savedBytes = minimalDocx("saved-after-restart");
    String latestKey =
        "native-editor-poc/" + workspaceId + "/" + templateId + "/latest.docx";
    when(templates.findById(templateId)).thenReturn(Optional.of(template));
    when(access.require(userId, workspaceId, WorkspacePermission.EDIT_CONTENT))
        .thenReturn(membership);
    when(membership.user()).thenReturn(user);
    when(user.displayName()).thenReturn("本地管理员");
    when(sourceService.getOriginalContent(userId, sourceVersionId))
        .thenReturn(new BinaryContent(originalBytes, "application/octet-stream", "合同.docx", "etag"));
    when(storage.stat("docmind-templates", latestKey))
        .thenReturn(new ObjectStorage.StoredObject(savedBytes.length, "saved-etag", DOCX_MIME));
    when(storage.open("docmind-templates", latestKey))
        .thenReturn(new ByteArrayInputStream(savedBytes));

    NativeEditorSessionService service =
        new NativeEditorSessionService(
            templates,
            access,
            sourceService,
            storage,
            storageProperties(),
            properties,
            jwtService,
            Clock.fixed(now, ZoneOffset.UTC));

    NativeEditorSessionResponse response = service.create(userId, templateId);
    @SuppressWarnings("unchecked")
    Map<String, Object> document = (Map<String, Object>) response.editorConfig().get("document");
    URI documentUri = URI.create((String) document.get("url"));
    String accessToken = documentUri.getQuery().substring("access_token=".length());

    assertThat(service.content(response.sessionId(), accessToken).bytes()).isEqualTo(savedBytes);
  }

  private NativeEditorProperties properties() {
    return new NativeEditorProperties(
        true,
        "http://127.0.0.1:8082",
        "http://127.0.0.1:8082",
        "http://host.docker.internal:8080",
        SECRET,
        "AuthorizationJwt",
        Duration.ofHours(2),
        10L * 1024L * 1024L,
        List.of("http://127.0.0.1:8082", "http://localhost"));
  }

  private DocmindStorageProperties storageProperties() {
    return new DocmindStorageProperties(
        "http://127.0.0.1:9000",
        "minioadmin",
        "12345678",
        Duration.ofMinutes(15),
        new DocmindStorageProperties.Buckets(
            "docmind-sources", "docmind-previews", "docmind-templates", "docmind-exports"));
  }

  private byte[] minimalDocx() throws Exception {
    return minimalDocx("");
  }

  private byte[] minimalDocx(String text) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(output)) {
      zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
      zip.write("<Types/>".getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
      zip.putNextEntry(new ZipEntry("word/document.xml"));
      zip.write(
          ("<w:document xmlns:w=\"urn:test\"><w:body><w:p>"
                  + text
                  + "</w:p></w:body></w:document>")
              .getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return output.toByteArray();
  }
}
