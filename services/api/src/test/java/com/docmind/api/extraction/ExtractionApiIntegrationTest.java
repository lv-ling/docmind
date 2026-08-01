package com.docmind.api.extraction;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.docmind.api.audit.infrastructure.AuditEventRepository;
import com.docmind.api.extraction.domain.ExtractionRunStatus;
import com.docmind.api.extraction.infrastructure.AsyncJobRepository;
import com.docmind.api.extraction.infrastructure.ExtractionCandidateRepository;
import com.docmind.api.extraction.infrastructure.ExtractionEvidenceRepository;
import com.docmind.api.extraction.infrastructure.ExtractionFieldResultRepository;
import com.docmind.api.extraction.infrastructure.ExtractionRunRepository;
import com.docmind.api.identity.domain.UserAccount;
import com.docmind.api.identity.domain.Workspace;
import com.docmind.api.identity.domain.WorkspaceMember;
import com.docmind.api.identity.domain.WorkspaceRole;
import com.docmind.api.identity.infrastructure.UserAccountRepository;
import com.docmind.api.identity.infrastructure.WorkspaceMemberRepository;
import com.docmind.api.identity.infrastructure.WorkspaceRepository;
import com.docmind.api.schema.infrastructure.ExtractionSchemaFieldRepository;
import com.docmind.api.schema.infrastructure.ExtractionSchemaRepository;
import com.docmind.api.schema.infrastructure.ExtractionSchemaVersionRepository;
import com.docmind.api.schema.infrastructure.SchemaTemplateRepository;
import com.docmind.api.sensitive.infrastructure.SensitiveRuleRepository;
import com.docmind.api.sensitive.infrastructure.SensitiveRuleTemplateRepository;
import com.docmind.api.sensitive.infrastructure.SensitiveRuleTemplateVersionRepository;
import com.docmind.api.source.domain.SourceDocument;
import com.docmind.api.source.domain.SourceFileType;
import com.docmind.api.source.domain.SourceVersion;
import com.docmind.api.source.infrastructure.SourceDocumentRepository;
import com.docmind.api.source.infrastructure.SourcePreviewRepository;
import com.docmind.api.source.infrastructure.SourceUploadSessionRepository;
import com.docmind.api.source.infrastructure.SourceVersionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExtractionApiIntegrationTest {

  private static final String PASSWORD = "SecurePass123!";
  private static final String UUID_PATTERN =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private UserAccountRepository users;
  @Autowired private WorkspaceRepository workspaces;
  @Autowired private WorkspaceMemberRepository members;
  @Autowired private AuditEventRepository audits;
  @Autowired private SourceDocumentRepository sources;
  @Autowired private SourceVersionRepository sourceVersions;
  @Autowired private SourcePreviewRepository sourcePreviews;
  @Autowired private SourceUploadSessionRepository uploadSessions;
  @Autowired private ExtractionSchemaRepository schemas;
  @Autowired private ExtractionSchemaVersionRepository schemaVersions;
  @Autowired private ExtractionSchemaFieldRepository schemaFields;
  @Autowired private SchemaTemplateRepository schemaTemplates;
  @Autowired private SensitiveRuleTemplateRepository sensitiveTemplates;
  @Autowired private SensitiveRuleTemplateVersionRepository sensitiveVersions;
  @Autowired private SensitiveRuleRepository sensitiveRules;
  @Autowired private AsyncJobRepository jobs;
  @Autowired private ExtractionRunRepository extractionRuns;
  @Autowired private ExtractionFieldResultRepository fieldResults;
  @Autowired private ExtractionCandidateRepository candidates;
  @Autowired private ExtractionEvidenceRepository evidence;

  @BeforeEach
  void cleanDatabase() {
    evidence.deleteAll();
    candidates.deleteAll();
    fieldResults.deleteAll();
    extractionRuns.deleteAll();
    jobs.deleteAll();
    sensitiveRules.deleteAll();
    sensitiveVersions.deleteAll();
    sensitiveTemplates.deleteAll();
    schemaTemplates.deleteAll();
    schemaFields.deleteAll();
    schemaVersions.deleteAll();
    schemas.deleteAll();
    sourcePreviews.deleteAll();
    uploadSessions.deleteAll();
    sourceVersions.deleteAll();
    sources.deleteAll();
    audits.deleteAll();
    members.deleteAll();
    workspaces.deleteAll();
    users.deleteAll();
  }

  @Test
  void createsQueuedExtractionIdempotentlyAndReturnsQueryableIds() throws Exception {
    TestContext context = createContext(false);
    UUID sourceVersionId = createSourceVersion(context, true);
    String schemaVersionId = createSchemaVersion(context);
    String request = extractionRequest(schemaVersionId);
    String idempotencyKey = UUID.randomUUID().toString();
    String requestId = UUID.randomUUID().toString();

    MvcResult accepted =
        mockMvc
            .perform(
                post("/api/v1/source-versions/{sourceVersionId}/extractions", sourceVersionId)
                    .header("Authorization", "Bearer " + context.ownerToken())
                    .header("Idempotency-Key", idempotencyKey)
                    .header("X-Request-ID", requestId)
                    .contentType(APPLICATION_JSON)
                    .content(request))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.job_id").value(matchesPattern(UUID_PATTERN)))
            .andExpect(jsonPath("$.extraction_id").value(matchesPattern(UUID_PATTERN)))
            .andExpect(jsonPath("$.request_id").value(requestId))
            .andReturn();
    JsonNode body = objectMapper.readTree(accepted.getResponse().getContentAsByteArray());
    String jobId = body.path("job_id").asText();
    String extractionId = body.path("extraction_id").asText();

    mockMvc
        .perform(
            post("/api/v1/source-versions/{sourceVersionId}/extractions", sourceVersionId)
                .header("Authorization", "Bearer " + context.ownerToken())
                .header("Idempotency-Key", idempotencyKey)
                .contentType(APPLICATION_JSON)
                .content(request))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.job_id").value(jobId))
        .andExpect(jsonPath("$.extraction_id").value(extractionId));

    mockMvc
        .perform(
            post("/api/v1/source-versions/{sourceVersionId}/extractions", sourceVersionId)
                .header("Authorization", "Bearer " + context.ownerToken())
                .header("Idempotency-Key", idempotencyKey)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"schema_version_id":"%s","sensitive_rule_template_version_id":"%s"}
                    """
                        .formatted(schemaVersionId, UUID.randomUUID())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

    mockMvc
        .perform(
            get("/api/v1/extractions/{extractionId}", extractionId)
                .header("Authorization", "Bearer " + context.ownerToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(extractionId))
        .andExpect(jsonPath("$.job_id").value(jobId))
        .andExpect(jsonPath("$.status").value("queued"))
        .andExpect(jsonPath("$.result").value(nullValue()))
        .andExpect(jsonPath("$.failure_code").value(nullValue()))
        .andExpect(jsonPath("$.completed_at").value(nullValue()));

    org.assertj.core.api.Assertions.assertThat(jobs.count()).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(extractionRuns.count()).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(
            extractionRuns.findById(UUID.fromString(extractionId)).orElseThrow().status())
        .isEqualTo(ExtractionRunStatus.QUEUED);
    org.assertj.core.api.Assertions.assertThat(fieldResults.count()).isZero();
    org.assertj.core.api.Assertions.assertThat(audits.count()).isEqualTo(2);
  }

  @Test
  void permitsViewerReadButRejectsViewerTaskCreation() throws Exception {
    TestContext context = createContext(true);
    UUID sourceVersionId = createSourceVersion(context, true);
    String schemaVersionId = createSchemaVersion(context);

    mockMvc
        .perform(
            post("/api/v1/source-versions/{sourceVersionId}/extractions", sourceVersionId)
                .header("Authorization", "Bearer " + context.viewerToken())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(APPLICATION_JSON)
                .content(extractionRequest(schemaVersionId)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

    MvcResult accepted =
        mockMvc
            .perform(
                post("/api/v1/source-versions/{sourceVersionId}/extractions", sourceVersionId)
                    .header("Authorization", "Bearer " + context.ownerToken())
                    .header("Idempotency-Key", UUID.randomUUID())
                    .contentType(APPLICATION_JSON)
                    .content(extractionRequest(schemaVersionId)))
            .andExpect(status().isAccepted())
            .andReturn();
    String extractionId =
        objectMapper
            .readTree(accepted.getResponse().getContentAsByteArray())
            .path("extraction_id")
            .asText();

    mockMvc
        .perform(
            get("/api/v1/extractions/{extractionId}", extractionId)
                .header("Authorization", "Bearer " + context.viewerToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("queued"));
  }

  @Test
  void rejectsExtractionBeforeSourceUploadCompletes() throws Exception {
    TestContext context = createContext(false);
    UUID sourceVersionId = createSourceVersion(context, false);
    String schemaVersionId = createSchemaVersion(context);

    mockMvc
        .perform(
            post("/api/v1/source-versions/{sourceVersionId}/extractions", sourceVersionId)
                .header("Authorization", "Bearer " + context.ownerToken())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(APPLICATION_JSON)
                .content(extractionRequest(schemaVersionId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    org.assertj.core.api.Assertions.assertThat(jobs.count()).isZero();
  }

  private String extractionRequest(String schemaVersionId) {
    return """
        {"schema_version_id":"%s","sensitive_rule_template_version_id":null}
        """
        .formatted(schemaVersionId);
  }

  private String createSchemaVersion(TestContext context) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/workspaces/{workspaceId}/schemas", context.workspaceId())
                    .header("Authorization", "Bearer " + context.ownerToken())
                    .header("Idempotency-Key", UUID.randomUUID())
                    .contentType(APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name":"Invoice schema",
                          "description":"Invoice fields",
                          "fields":[{
                            "key":"invoice_number",
                            "json_path":"$.invoice.number",
                            "description":"Invoice number",
                            "value_type":"string",
                            "array_item_type":null,
                            "required":true,
                            "nullable":false,
                            "default":{"kind":"none"},
                            "sensitivity":"none",
                            "constraints":{"format":null,"pattern":null,"enum_values":[],"min_length":null,"max_length":null,"minimum":null,"maximum":null},
                            "examples":[],
                            "extraction_hint":null,
                            "display":{"mask":"none","view_role_keys":[]},
                            "metadata":{},
                            "position":0
                          }]
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsByteArray())
        .path("current_version")
        .path("id")
        .asText();
  }

  private UUID createSourceVersion(TestContext context, boolean complete) {
    Instant now = Instant.now();
    UUID sourceId = UUID.randomUUID();
    UUID versionId = UUID.randomUUID();
    sources.saveAndFlush(
        new SourceDocument(
            sourceId, context.workspaceId(), "Invoice", context.ownerId(), now));
    SourceVersion version =
        new SourceVersion(
            versionId,
            sourceId,
            context.workspaceId(),
            1,
            "invoice.pdf",
            SourceFileType.PDF,
            "application/pdf",
            128,
            "sources",
            "staging/" + versionId,
            "sources",
            "immutable/" + versionId,
            context.ownerId(),
            now);
    if (complete) {
      version.complete("application/pdf", 128, "a".repeat(64), "etag", now);
    }
    sourceVersions.saveAndFlush(version);
    return versionId;
  }

  private TestContext createContext(boolean withViewer) throws Exception {
    UserAccount owner =
        users.saveAndFlush(
            new UserAccount("owner@example.com", "Owner", passwordEncoder.encode(PASSWORD)));
    Workspace workspace =
        workspaces.saveAndFlush(
            new Workspace(
                "Legal Team",
                "legal-" + UUID.randomUUID(),
                owner.id(),
                "seed",
                "0".repeat(64)));
    members.saveAndFlush(new WorkspaceMember(workspace, owner, WorkspaceRole.OWNER));
    String viewerToken = null;
    if (withViewer) {
      UserAccount viewer =
          users.saveAndFlush(
              new UserAccount("viewer@example.com", "Viewer", passwordEncoder.encode(PASSWORD)));
      members.saveAndFlush(new WorkspaceMember(workspace, viewer, WorkspaceRole.VIEWER));
      viewerToken = login(viewer.email());
    }
    return new TestContext(workspace.id(), owner.id(), login(owner.email()), viewerToken);
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
        .path("access_token")
        .asText();
  }

  private record TestContext(
      UUID workspaceId, UUID ownerId, String ownerToken, String viewerToken) {}
}
