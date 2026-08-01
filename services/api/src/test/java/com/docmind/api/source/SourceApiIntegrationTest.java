package com.docmind.api.source;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.docmind.api.audit.infrastructure.AuditEventRepository;
import com.docmind.api.extraction.domain.AsyncJobType;
import com.docmind.api.extraction.infrastructure.AsyncJobRepository;
import com.docmind.api.identity.domain.UserAccount;
import com.docmind.api.identity.domain.Workspace;
import com.docmind.api.identity.domain.WorkspaceMember;
import com.docmind.api.identity.domain.WorkspaceRole;
import com.docmind.api.identity.infrastructure.UserAccountRepository;
import com.docmind.api.identity.infrastructure.WorkspaceMemberRepository;
import com.docmind.api.identity.infrastructure.WorkspaceRepository;
import com.docmind.api.infrastructure.storage.ObjectStorage;
import com.docmind.api.infrastructure.storage.ObjectStorage.StoredObject;
import com.docmind.api.source.infrastructure.SourceDocumentRepository;
import com.docmind.api.source.infrastructure.SourcePreviewRepository;
import com.docmind.api.source.infrastructure.SourceUploadSessionRepository;
import com.docmind.api.source.infrastructure.SourceVersionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SourceApiIntegrationTest {

  private static final String PASSWORD = "SecurePass123!";
  private static final byte[] PDF =
      "%PDF-1.7\n1 0 obj\n<<>>\nendobj\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
  private static final String ETAG = "55d34a10b59f08b53d77e27ad5b2702a";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private UserAccountRepository users;
  @Autowired private WorkspaceRepository workspaces;
  @Autowired private WorkspaceMemberRepository members;
  @Autowired private AuditEventRepository audits;
  @Autowired private SourceDocumentRepository sources;
  @Autowired private SourceVersionRepository versions;
  @Autowired private SourceUploadSessionRepository uploads;
  @Autowired private SourcePreviewRepository previews;
  @Autowired private AsyncJobRepository jobs;

  @MockitoBean private ObjectStorage storage;

  @BeforeEach
  void cleanDatabaseAndStorage() {
    jobs.deleteAll();
    previews.deleteAll();
    uploads.deleteAll();
    versions.deleteAll();
    sources.deleteAll();
    audits.deleteAll();
    members.deleteAll();
    workspaces.deleteAll();
    users.deleteAll();
    reset(storage);
    when(storage.presignedPut(anyString(), anyString(), any()))
        .thenReturn("https://storage.example.test/upload?signature=redacted");
  }

  @Test
  void completesAnUploadIdempotentlyAndExposesVersionedReadOnlyAccess() throws Exception {
    Fixture fixture = fixture(WorkspaceRole.OWNER);
    MvcResult created = createPdfUpload(fixture, "create-key");
    JsonNode createdBody = objectMapper.readTree(created.getResponse().getContentAsByteArray());
    String sourceId = createdBody.get("source").get("id").asText();
    String versionId = createdBody.get("version").get("id").asText();

    mockMvc
        .perform(
            post("/api/v1/workspaces/{workspaceId}/sources", fixture.workspace().id())
                .header("Authorization", "Bearer " + fixture.token())
                .header("Idempotency-Key", "create-key")
                .contentType(APPLICATION_JSON)
                .content(createRequest()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.source.id").value(sourceId))
        .andExpect(jsonPath("$.version.file").doesNotExist());

    prepareStoredPdf();
    String completeRequest = completeRequest(PDF, ETAG, "application/pdf");
    mockMvc
        .perform(
            post("/api/v1/source-versions/{versionId}/complete", versionId)
                .header("Authorization", "Bearer " + fixture.token())
                .header("Idempotency-Key", "complete-key")
                .contentType(APPLICATION_JSON)
                .content(completeRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.source.current_version_id").value(versionId))
        .andExpect(jsonPath("$.version.status").value("uploaded"))
        .andExpect(jsonPath("$.version.file.mime_type").value("application/pdf"))
        .andExpect(jsonPath("$.version.file.sha256").value(sha256(PDF)));

    org.assertj.core.api.Assertions.assertThat(jobs.findAll())
        .singleElement()
        .satisfies(
            job -> {
              org.assertj.core.api.Assertions.assertThat(job.jobType())
                  .isEqualTo(AsyncJobType.SOURCE_PREVIEW);
              org.assertj.core.api.Assertions.assertThat(job.aggregateType())
                  .isEqualTo("source_preview");
            });

    verify(storage)
        .copyIfMatch(
            anyString(),
            argThat(key -> key.startsWith("staging/")),
            org.mockito.ArgumentMatchers.eq(ETAG),
            anyString(),
            argThat(key -> key.startsWith("immutable/")));
    verify(storage).delete(anyString(), argThat(key -> key.startsWith("staging/")));

    mockMvc
        .perform(
            post("/api/v1/source-versions/{versionId}/complete", versionId)
                .header("Authorization", "Bearer " + fixture.token())
                .header("Idempotency-Key", "complete-key")
                .contentType(APPLICATION_JSON)
                .content(completeRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version.id").value(versionId));

    mockMvc
        .perform(
            post("/api/v1/workspaces/{workspaceId}/sources", fixture.workspace().id())
                .header("Authorization", "Bearer " + fixture.token())
                .header("Idempotency-Key", "create-key")
                .contentType(APPLICATION_JSON)
                .content(createRequest()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.upload.status").value("completed"))
        .andExpect(jsonPath("$.upload.upload_url").doesNotExist());

    mockMvc
        .perform(
            get("/api/v1/workspaces/{workspaceId}/sources", fixture.workspace().id())
                .header("Authorization", "Bearer " + fixture.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(sourceId))
        .andExpect(jsonPath("$.has_more").value(false));

    mockMvc
        .perform(
            get("/api/v1/sources/{sourceId}", sourceId)
                .header("Authorization", "Bearer " + fixture.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versions[0].version_number").value(1));

    mockMvc
        .perform(
            get("/api/v1/source-versions/{versionId}/preview", versionId)
                .header("Authorization", "Bearer " + fixture.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.preview.status").value("queued"))
        .andExpect(jsonPath("$.view_url").doesNotExist())
        .andExpect(jsonPath("$.original_content_url").value(startsWith("/api/v1/")));

    mockMvc
        .perform(
            get("/api/v1/source-versions/{versionId}/content", versionId)
                .header("Authorization", "Bearer " + fixture.token()))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/pdf"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(content().bytes(PDF));

    org.assertj.core.api.Assertions.assertThat(audits.count()).isEqualTo(2);
  }

  @Test
  void createsTheNextImmutableVersionAndRejectsAReusedKeyWithDifferentInput() throws Exception {
    Fixture fixture = fixture(WorkspaceRole.OWNER);
    JsonNode first =
        objectMapper.readTree(
            createPdfUpload(fixture, "create-key").getResponse().getContentAsByteArray());
    String sourceId = first.get("source").get("id").asText();

    String request =
        objectMapper.writeValueAsString(
            Map.of(
                "original_file_name", "second.pdf",
                "declared_mime_type", "application/pdf",
                "size_bytes", PDF.length));
    mockMvc
        .perform(
            post("/api/v1/sources/{sourceId}/versions", sourceId)
                .header("Authorization", "Bearer " + fixture.token())
                .header("Idempotency-Key", "version-key")
                .contentType(APPLICATION_JSON)
                .content(request))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.version.version_number").value(2));

    mockMvc
        .perform(
            post("/api/v1/sources/{sourceId}/versions", sourceId)
                .header("Authorization", "Bearer " + fixture.token())
                .header("Idempotency-Key", "version-key")
                .contentType(APPLICATION_JSON)
                .content(
                    request.replace("second.pdf", "different.pdf")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
  }

  @Test
  void rejectsOversizedMismatchedAndSpoofedFilesBeforeTheyBecomeCurrent() throws Exception {
    Fixture fixture = fixture(WorkspaceRole.OWNER);
    mockMvc
        .perform(
            post("/api/v1/workspaces/{workspaceId}/sources", fixture.workspace().id())
                .header("Authorization", "Bearer " + fixture.token())
                .header("Idempotency-Key", "oversized-key")
                .contentType(APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "document_name", "Too large",
                            "original_file_name", "large.pdf",
                            "declared_mime_type", "application/pdf",
                            "size_bytes", 10_485_761))))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));

    mockMvc
        .perform(
            post("/api/v1/workspaces/{workspaceId}/sources", fixture.workspace().id())
                .header("Authorization", "Bearer " + fixture.token())
                .header("Idempotency-Key", "mime-key")
                .contentType(APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "document_name", "Mismatch",
                            "original_file_name", "mismatch.pdf",
                            "declared_mime_type", "application/msword",
                            "size_bytes", PDF.length))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("FILE_TYPE_NOT_ALLOWED"));

    JsonNode created =
        objectMapper.readTree(
            createPdfUpload(fixture, "spoof-key").getResponse().getContentAsByteArray());
    String versionId = created.get("version").get("id").asText();
    byte[] html = "<html>not a pdf</html>".getBytes(StandardCharsets.UTF_8);
    when(storage.stat(anyString(), anyString()))
        .thenReturn(new StoredObject(PDF.length, ETAG, "application/pdf"));
    when(storage.open(anyString(), anyString())).thenReturn(new ByteArrayInputStream(html));

    mockMvc
        .perform(
            post("/api/v1/source-versions/{versionId}/complete", versionId)
                .header("Authorization", "Bearer " + fixture.token())
                .header("Idempotency-Key", "complete-spoof")
                .contentType(APPLICATION_JSON)
                .content(completeRequest(PDF, ETAG, "application/pdf")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void enforcesEditorWriteAndViewerReadPermissionsAtObjectLevel() throws Exception {
    UserAccount owner = createUser("owner@example.com", "Owner");
    UserAccount viewer = createUser("viewer@example.com", "Viewer");
    Workspace workspace = createWorkspace(owner);
    members.saveAndFlush(new WorkspaceMember(workspace, owner, WorkspaceRole.OWNER));
    members.saveAndFlush(new WorkspaceMember(workspace, viewer, WorkspaceRole.VIEWER));
    String viewerToken = login(viewer.email());

    mockMvc
        .perform(
            post("/api/v1/workspaces/{workspaceId}/sources", workspace.id())
                .header("Authorization", "Bearer " + viewerToken)
                .header("Idempotency-Key", "viewer-key")
                .contentType(APPLICATION_JSON)
                .content(createRequest()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

    mockMvc
        .perform(
            get("/api/v1/workspaces/{workspaceId}/sources", workspace.id())
                .header("Authorization", "Bearer " + viewerToken))
        .andExpect(status().isOk());
  }

  private MvcResult createPdfUpload(Fixture fixture, String key) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/workspaces/{workspaceId}/sources", fixture.workspace().id())
                .header("Authorization", "Bearer " + fixture.token())
                .header("Idempotency-Key", key)
                .contentType(APPLICATION_JSON)
                .content(createRequest()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.version.status").value("uploading"))
        .andExpect(jsonPath("$.version.file").doesNotExist())
        .andExpect(jsonPath("$.upload.upload_method").value("PUT"))
        .andExpect(jsonPath("$.upload.upload_url").value(startsWith("https://")))
        .andReturn();
  }

  private String createRequest() throws Exception {
    return objectMapper.writeValueAsString(
        Map.of(
            "document_name", "Contract",
            "original_file_name", "contract.pdf",
            "declared_mime_type", "application/pdf",
            "size_bytes", PDF.length));
  }

  private String completeRequest(byte[] bytes, String etag, String mimeType) throws Exception {
    return objectMapper.writeValueAsString(
        Map.of(
            "size_bytes", PDF.length,
            "detected_mime_type", mimeType,
            "sha256", sha256(bytes),
            "object_etag", etag));
  }

  private void prepareStoredPdf() {
    when(storage.stat(anyString(), anyString()))
        .thenReturn(new StoredObject(PDF.length, ETAG, "application/octet-stream"));
    when(storage.open(anyString(), anyString()))
        .thenAnswer(invocation -> new ByteArrayInputStream(PDF));
  }

  private Fixture fixture(WorkspaceRole role) throws Exception {
    UserAccount user = createUser("user@example.com", "User");
    Workspace workspace = createWorkspace(user);
    members.saveAndFlush(new WorkspaceMember(workspace, user, role));
    return new Fixture(workspace, login(user.email()));
  }

  private UserAccount createUser(String email, String displayName) {
    return users.saveAndFlush(
        new UserAccount(email, displayName, passwordEncoder.encode(PASSWORD)));
  }

  private Workspace createWorkspace(UserAccount owner) {
    return workspaces.saveAndFlush(
        new Workspace(
            "Legal Team",
            "legal-" + UUID.randomUUID().toString().substring(0, 8),
            owner.id(),
            UUID.randomUUID().toString(),
            "0".repeat(64)));
  }

  private String login(String email) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("email", email, "password", PASSWORD))))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsByteArray())
        .get("access_token")
        .asText();
  }

  private String sha256(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

  private record Fixture(Workspace workspace, String token) {}
}
