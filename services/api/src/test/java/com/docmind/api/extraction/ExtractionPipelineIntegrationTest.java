package com.docmind.api.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.docmind.api.audit.infrastructure.AuditEventRepository;
import com.docmind.api.extraction.ai.AiServiceClient;
import com.docmind.api.extraction.ai.AiServiceContracts.AiExtractionRequest;
import com.docmind.api.extraction.ai.AiServiceContracts.AiExtractionResponse;
import com.docmind.api.extraction.ai.AiServiceContracts.ExtractionModelMetadata;
import com.docmind.api.extraction.ai.AiServiceContracts.ModelCandidateOutput;
import com.docmind.api.extraction.ai.AiServiceContracts.ModelEvidence;
import com.docmind.api.extraction.ai.AiServiceContracts.ModelExtractionOutput;
import com.docmind.api.extraction.ai.AiServiceContracts.ModelFieldOutput;
import com.docmind.api.extraction.ai.AiServiceContracts.ParseDocumentResponse;
import com.docmind.api.extraction.ai.AiServiceContracts.ParsedTextNode;
import com.docmind.api.extraction.ai.AiServiceContracts.SensitiveTextSpan;
import com.docmind.api.extraction.ai.AiServiceContracts.SensitiveTokenReference;
import com.docmind.api.extraction.ai.AiServiceContracts.SensitiveTokenizationResponse;
import com.docmind.api.extraction.ai.AiServiceContracts.TokenizedSensitiveTextNode;
import com.docmind.api.extraction.application.ExtractionJobHandler;
import com.docmind.api.extraction.application.ExtractionReviewService;
import com.docmind.api.extraction.api.ApproveExtractionRequest;
import com.docmind.api.extraction.api.ReviewExtractionFieldRequest;
import com.docmind.api.extraction.domain.AsyncJob;
import com.docmind.api.extraction.domain.AsyncJobType;
import com.docmind.api.extraction.domain.ExtractionRun;
import com.docmind.api.extraction.domain.ExtractionRunStatus;
import com.docmind.api.extraction.infrastructure.AsyncJobRepository;
import com.docmind.api.extraction.infrastructure.ExtractionCandidateRepository;
import com.docmind.api.extraction.infrastructure.ExtractionEvidenceRepository;
import com.docmind.api.extraction.infrastructure.ExtractionFieldResultRepository;
import com.docmind.api.extraction.infrastructure.ExtractionRunRepository;
import com.docmind.api.extraction.infrastructure.ExtractionReviewOperationRepository;
import com.docmind.api.extraction.infrastructure.SensitiveTokenRepository;
import com.docmind.api.extraction.messaging.AsyncJobCommand;
import com.docmind.api.identity.domain.UserAccount;
import com.docmind.api.identity.domain.Workspace;
import com.docmind.api.identity.domain.WorkspaceMember;
import com.docmind.api.identity.domain.WorkspaceRole;
import com.docmind.api.identity.infrastructure.UserAccountRepository;
import com.docmind.api.identity.infrastructure.WorkspaceMemberRepository;
import com.docmind.api.identity.infrastructure.WorkspaceRepository;
import com.docmind.api.infrastructure.crypto.JsonEnvelopeEncryption;
import com.docmind.api.infrastructure.storage.ObjectStorage;
import com.docmind.api.schema.domain.ExtractionSchema;
import com.docmind.api.schema.domain.ExtractionSchemaField;
import com.docmind.api.schema.domain.ExtractionSchemaVersion;
import com.docmind.api.schema.domain.FieldSensitivity;
import com.docmind.api.schema.domain.SchemaFieldDefaultKind;
import com.docmind.api.schema.domain.SchemaValueType;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "docmind.ai.enabled=true")
@ActiveProfiles("test")
class ExtractionPipelineIntegrationTest {

  private static final byte[] PDF =
      "%PDF-1.7\nmock\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
  private static final String FIRST_PHONE = "+86 13800138000";
  private static final String SECOND_PHONE = "+86 13900139000";
  private static final String FIRST_TOKEN = "[[SENSITIVE:PHONE_NUMBER:01]]";
  private static final String SECOND_TOKEN = "[[SENSITIVE:PHONE_NUMBER:02]]";

  @Autowired private ExtractionJobHandler handler;
  @Autowired private ExtractionReviewService reviews;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JsonEnvelopeEncryption encryption;
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
  @Autowired private ExtractionRunRepository runs;
  @Autowired private ExtractionReviewOperationRepository reviewOperations;
  @Autowired private ExtractionFieldResultRepository fieldResults;
  @Autowired private ExtractionCandidateRepository candidates;
  @Autowired private ExtractionEvidenceRepository evidence;
  @Autowired private SensitiveTokenRepository tokens;

  @MockitoBean private ObjectStorage storage;
  @MockitoBean private AiServiceClient ai;

  @BeforeEach
  void cleanDatabase() {
    reviewOperations.deleteAll();
    evidence.deleteAll();
    candidates.deleteAll();
    fieldResults.deleteAll();
    tokens.deleteAll();
    runs.deleteAll();
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
    reset(storage, ai);
  }

  @Test
  void parsesTokenizesValidatesRestoresAndPersistsAmbiguousSensitiveResults() {
    Fixture fixture = fixture();
    String originalText = "Phone: " + FIRST_PHONE + "; Phone: " + SECOND_PHONE;
    String tokenizedText = "Phone: " + FIRST_TOKEN + "; Phone: " + SECOND_TOKEN;
    ParseDocumentResponse parsed = parsed(fixture.source().id(), originalText);
    SensitiveTokenizationResponse tokenized =
        tokenized(fixture.source().id(), originalText, tokenizedText);

    when(storage.open(fixture.source().objectBucket(), fixture.source().objectKey()))
        .thenReturn(new ByteArrayInputStream(PDF));
    when(ai.parse(
            eq(fixture.source().id()),
            eq("pdf"),
            eq("und"),
            eq("contact.pdf"),
            any(byte[].class),
            eq(fixture.command().requestId())))
        .thenReturn(parsed);
    when(ai.tokenize(any(), eq(fixture.command().requestId()))).thenReturn(tokenized);
    when(ai.extract(any(AiExtractionRequest.class)))
        .thenAnswer(
            invocation -> extractionResponse(invocation.getArgument(0), tokenizedText));

    handler.handle(fixture.command());

    ExtractionRun completed = runs.findById(fixture.run().id()).orElseThrow();
    assertThat(completed.status()).isEqualTo(ExtractionRunStatus.REVIEW_REQUIRED);
    assertThat(completed.containsSensitiveValues()).isTrue();
    assertThat(completed.modelProvider()).isEqualTo("mock");
    assertThat(tokens.count()).isEqualTo(2);
    assertThat(fieldResults.count()).isEqualTo(1);
    assertThat(candidates.count()).isEqualTo(2);
    assertThat(evidence.count()).isEqualTo(3);

    var field = fieldResults.findAllByExtractionRunIdOrderByJsonPathAsc(completed.id()).get(0);
    assertThat(field.needsReview()).isTrue();
    assertThat(field.maskedPreview()).isEqualTo("[敏感内容]");
    assertThat(
            encryption.decrypt(
                field.valueEnvelope(),
                "extraction-field:" + completed.id() + ":" + fixture.schemaField().id()))
        .isEqualTo(objectMapper.getNodeFactory().textNode(FIRST_PHONE));
    assertThat(
            encryption.decrypt(
                completed.resultDataEnvelope(), "extraction-result:" + completed.id()))
        .isEqualTo(objectMapper.valueToTree(Map.of("contact", Map.of("phone", FIRST_PHONE))));

    assertThat(completed.resultDataEnvelope().toString()).doesNotContain(FIRST_PHONE, SECOND_PHONE);
    assertThat(field.valueEnvelope().toString()).doesNotContain(FIRST_PHONE, SECOND_PHONE);
    assertThat(tokens.findAllByExtractionRunIdOrderByTokenAsc(completed.id()))
        .allSatisfy(
            token ->
                assertThat(token.valueEnvelope().toString())
                    .doesNotContain(FIRST_PHONE, SECOND_PHONE));
  }

  @Test
  void masksByRoleThenRecordsIdempotentModificationAndApprovesSchemaValidResult() {
    Fixture fixture = fixture();
    String originalText = "Phone: " + FIRST_PHONE + "; Phone: " + SECOND_PHONE;
    String tokenizedText = "Phone: " + FIRST_TOKEN + "; Phone: " + SECOND_TOKEN;
    when(storage.open(fixture.source().objectBucket(), fixture.source().objectKey()))
        .thenReturn(new ByteArrayInputStream(PDF));
    when(ai.parse(
            eq(fixture.source().id()),
            eq("pdf"),
            eq("und"),
            eq("contact.pdf"),
            any(byte[].class),
            eq(fixture.command().requestId())))
        .thenReturn(parsed(fixture.source().id(), originalText));
    when(ai.tokenize(any(), eq(fixture.command().requestId())))
        .thenReturn(tokenized(fixture.source().id(), originalText, tokenizedText));
    when(ai.extract(any(AiExtractionRequest.class)))
        .thenAnswer(invocation -> extractionResponse(invocation.getArgument(0), tokenizedText));
    handler.handle(fixture.command());

    var ownerView = reviews.get(fixture.ownerId(), fixture.run().id());
    var viewerView = reviews.get(fixture.viewerId(), fixture.run().id());
    assertThat(ownerView.result().fields().get(0).displayValue().path("access").asText())
        .isEqualTo("visible");
    assertThat(ownerView.result().fields().get(0).displayValue().path("value").asText())
        .isEqualTo(FIRST_PHONE);
    assertThat(ownerView.result().fields().get(0).evidence().get(0).isMasked()).isFalse();
    assertThat(viewerView.result().fields().get(0).displayValue().path("access").asText())
        .isEqualTo("masked");
    assertThat(viewerView.result().fields().get(0).displayValue().toString())
        .doesNotContain(FIRST_PHONE, SECOND_PHONE);
    assertThat(viewerView.result().fields().get(0).evidence().get(0).isMasked()).isTrue();

    var field = fieldResults.findAllByExtractionRunIdOrderByJsonPathAsc(fixture.run().id()).get(0);
    UUID reviewRequestId = UUID.randomUUID();
    String reviewKey = UUID.randomUUID().toString();
    var modified =
        reviews.reviewField(
            fixture.ownerId(),
            fixture.run().id(),
            field.id(),
            new ReviewExtractionFieldRequest(
                "modify", objectMapper.getNodeFactory().textNode(SECOND_PHONE), "候选值更准确"),
            reviewKey,
            reviewRequestId);
    assertThat(modified.result().fields().get(0).reviewStatus()).isEqualTo("modified");
    assertThat(modified.result().fields().get(0).valueSource()).isEqualTo("manual");
    assertThat(modified.result().fields().get(0).displayValue().path("value").asText())
        .isEqualTo(SECOND_PHONE);
    var storedModified = fieldResults.findById(field.id()).orElseThrow();
    assertThat(storedModified.reviewedValueEnvelope().toString()).doesNotContain(SECOND_PHONE);

    String approvalKey = UUID.randomUUID().toString();
    var approved =
        reviews.approve(
            fixture.ownerId(),
            fixture.run().id(),
            new ApproveExtractionRequest("复核完成"),
            approvalKey,
            UUID.randomUUID());
    assertThat(approved.status()).isEqualTo("approved");
    assertThat(approved.result().data().path("contact").path("phone").asText())
        .isEqualTo(SECOND_PHONE);
    assertThat(reviewOperations.count()).isEqualTo(2);

    var replayed =
        reviews.approve(
            fixture.ownerId(),
            fixture.run().id(),
            new ApproveExtractionRequest("复核完成"),
            approvalKey,
            UUID.randomUUID());
    assertThat(replayed.status()).isEqualTo("approved");
    assertThat(reviewOperations.count()).isEqualTo(2);
  }

  private Fixture fixture() {
    Instant now = Instant.parse("2026-08-01T00:00:00Z");
    UserAccount user =
        users.saveAndFlush(new UserAccount("pipeline@example.com", "Pipeline", "hash"));
    UserAccount viewer =
        users.saveAndFlush(new UserAccount("pipeline-viewer@example.com", "Viewer", "hash"));
    Workspace workspace =
        workspaces.saveAndFlush(
            new Workspace(
                "Pipeline", "pipeline-" + UUID.randomUUID(), user.id(), "seed", "0".repeat(64)));
    members.saveAndFlush(new WorkspaceMember(workspace, user, WorkspaceRole.OWNER));
    members.saveAndFlush(new WorkspaceMember(workspace, viewer, WorkspaceRole.VIEWER));
    UUID sourceId = UUID.randomUUID();
    UUID sourceVersionId = UUID.randomUUID();
    sources.saveAndFlush(new SourceDocument(sourceId, workspace.id(), "Contact", user.id(), now));
    SourceVersion source =
        new SourceVersion(
            sourceVersionId,
            sourceId,
            workspace.id(),
            1,
            "contact.pdf",
            SourceFileType.PDF,
            "application/pdf",
            PDF.length,
            "test-sources",
            "staging/" + sourceVersionId,
            "test-sources",
            "immutable/" + sourceVersionId,
            user.id(),
            now);
    source.complete("application/pdf", PDF.length, sha256(PDF), "etag", now);
    source = sourceVersions.saveAndFlush(source);

    ExtractionSchema schema =
        schemas.saveAndFlush(
            new ExtractionSchema(
                workspace.id(), "Contact", "Contact schema", user.id(), "schema", "1".repeat(64), now));
    ObjectNode jsonSchema = objectMapper.createObjectNode();
    jsonSchema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
    jsonSchema.put("type", "object");
    jsonSchema.put("additionalProperties", false);
    ObjectNode contact = jsonSchema.putObject("properties").putObject("contact");
    contact.put("type", "object");
    contact.put("additionalProperties", false);
    contact.putObject("properties").putObject("phone").put("type", "string");
    contact.putArray("required").add("phone");
    jsonSchema.putArray("required").add("contact");
    ExtractionSchemaVersion schemaVersion =
        schemaVersions.saveAndFlush(
            new ExtractionSchemaVersion(
                schema.id(),
                workspace.id(),
                1,
                jsonSchema,
                "initial",
                user.id(),
                "version",
                "2".repeat(64),
                now));
    ArrayNode emptyArray = objectMapper.createArrayNode();
    ObjectNode display = objectMapper.createObjectNode();
    display.put("mask", "full");
    display.putArray("view_role_keys").add("owner");
    ExtractionSchemaField schemaField =
        schemaFields.saveAndFlush(
            new ExtractionSchemaField(
                schemaVersion.id(),
                "phone",
                "$.contact.phone",
                "Phone",
                SchemaValueType.STRING,
                null,
                true,
                false,
                SchemaFieldDefaultKind.NONE,
                null,
                FieldSensitivity.HIGH,
                objectMapper.createObjectNode(),
                emptyArray,
                null,
                display,
                objectMapper.createObjectNode(),
                0));

    UUID runId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    AsyncJob job =
        jobs.saveAndFlush(
            new AsyncJob(
                jobId,
                workspace.id(),
                AsyncJobType.EXTRACTION,
                "extraction_run",
                runId,
                3,
                UUID.randomUUID(),
                user.id(),
                now));
    ExtractionRun run =
        runs.saveAndFlush(
            new ExtractionRun(
                runId,
                jobId,
                workspace.id(),
                source.id(),
                schemaVersion.id(),
                null,
                user.id(),
                "extract",
                "3".repeat(64),
                objectMapper.createArrayNode(),
                now));
    return new Fixture(
        source, schemaField, run, AsyncJobCommand.from(job), user.id(), viewer.id());
  }

  private ParseDocumentResponse parsed(UUID sourceVersionId, String originalText) {
    ObjectNode document = objectMapper.createObjectNode();
    document.putObject("metadata").put("language", "en");
    return new ParseDocumentResponse(
        sourceVersionId,
        "pdf",
        "test-parser",
        document,
        List.of(new ParsedTextNode("node-1", "paragraph", 1, originalText, Map.of())),
        List.of(),
        List.of());
  }

  private SensitiveTokenizationResponse tokenized(
      UUID sourceVersionId, String originalText, String tokenizedText) {
    int firstStart = originalText.indexOf(FIRST_PHONE);
    int secondStart = originalText.indexOf(SECOND_PHONE);
    return new SensitiveTokenizationResponse(
        sourceVersionId,
        List.of(
            new TokenizedSensitiveTextNode(
                "node-1", "paragraph", 1, tokenizedText, Map.of())),
        List.of(
            new SensitiveTokenReference(
                UUID.randomUUID(),
                sourceVersionId,
                FIRST_TOKEN,
                "phone_number",
                "+86 138****8000",
                List.of(
                    new SensitiveTextSpan(
                        "node-1", firstStart, firstStart + FIRST_PHONE.length()))),
            new SensitiveTokenReference(
                UUID.randomUUID(),
                sourceVersionId,
                SECOND_TOKEN,
                "phone_number",
                "+86 139****9000",
                List.of(
                    new SensitiveTextSpan(
                        "node-1", secondStart, secondStart + SECOND_PHONE.length())))),
        List.of());
  }

  private AiExtractionResponse extractionResponse(
      AiExtractionRequest request, String tokenizedText) {
    ModelEvidence modelEvidence = new ModelEvidence("node-1", 1, tokenizedText);
    ModelFieldOutput field =
        new ModelFieldOutput(
            "$.contact.phone",
            objectMapper.getNodeFactory().textNode(FIRST_TOKEN),
            new java.math.BigDecimal("0.76"),
            List.of(modelEvidence),
            List.of(
                new ModelCandidateOutput(
                    objectMapper.getNodeFactory().textNode(FIRST_TOKEN),
                    new java.math.BigDecimal("0.76"),
                    List.of(modelEvidence)),
                new ModelCandidateOutput(
                    objectMapper.getNodeFactory().textNode(SECOND_TOKEN),
                    new java.math.BigDecimal("0.74"),
                    List.of(modelEvidence))),
            true);
    ObjectNode data = objectMapper.createObjectNode();
    data.putObject("contact").put("phone", FIRST_TOKEN);
    return new AiExtractionResponse(
        request.requestId(),
        request.jobId(),
        request.extractionRunId(),
        new ModelExtractionOutput(data, List.of(field)),
        new ExtractionModelMetadata("mock", "deterministic", "mock-v1", 120, 40),
        List.of());
  }

  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private record Fixture(
      SourceVersion source,
      ExtractionSchemaField schemaField,
      ExtractionRun run,
      AsyncJobCommand command,
      UUID ownerId,
      UUID viewerId) {}
}
