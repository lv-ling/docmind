package com.docmind.api.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.docmind.api.audit.infrastructure.AuditEventRepository;
import com.docmind.api.extraction.ai.AiServiceClient;
import com.docmind.api.extraction.ai.AiServiceContracts.ParseDocumentResponse;
import com.docmind.api.extraction.ai.AiServiceContracts.ParsedTextNode;
import com.docmind.api.extraction.infrastructure.AsyncJobRepository;
import com.docmind.api.extraction.messaging.AsyncJobCommand;
import com.docmind.api.identity.domain.UserAccount;
import com.docmind.api.identity.domain.Workspace;
import com.docmind.api.identity.domain.WorkspaceMember;
import com.docmind.api.identity.domain.WorkspaceRole;
import com.docmind.api.identity.infrastructure.UserAccountRepository;
import com.docmind.api.identity.infrastructure.WorkspaceMemberRepository;
import com.docmind.api.identity.infrastructure.WorkspaceRepository;
import com.docmind.api.infrastructure.storage.ObjectStorage;
import com.docmind.api.source.domain.SourceDocument;
import com.docmind.api.source.domain.SourceFileType;
import com.docmind.api.source.domain.SourcePreview;
import com.docmind.api.source.domain.SourcePreviewStatus;
import com.docmind.api.source.domain.SourceVersion;
import com.docmind.api.source.infrastructure.SourceDocumentRepository;
import com.docmind.api.source.infrastructure.SourcePreviewRepository;
import com.docmind.api.source.infrastructure.SourceVersionRepository;
import com.docmind.api.template.api.CreateTemplateRequest;
import com.docmind.api.template.api.CreateTemplateVersionRequest;
import com.docmind.api.template.api.PublishTemplateVersionRequest;
import com.docmind.api.template.api.RollbackTemplateRequest;
import com.docmind.api.template.application.DocumentPreviewConverter;
import com.docmind.api.template.application.DocumentTemplateService;
import com.docmind.api.template.application.TemplateConversionJobHandler;
import com.docmind.api.template.domain.TemplateVersionStatus;
import com.docmind.api.template.infrastructure.DocumentConversionWarningRepository;
import com.docmind.api.template.infrastructure.DocumentTemplateOperationRepository;
import com.docmind.api.template.infrastructure.DocumentTemplateRepository;
import com.docmind.api.template.infrastructure.DocumentTemplateResourceRepository;
import com.docmind.api.template.infrastructure.DocumentTemplateVersionRepository;
import com.docmind.api.template.infrastructure.ParsedContentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
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
class TemplateLifecycleIntegrationTest {
  private static final byte[] PDF = "%PDF-1.7\ntemplate\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] IMAGE = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47};

  @Autowired private TemplateConversionJobHandler handler;
  @Autowired private DocumentTemplateService service;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserAccountRepository users;
  @Autowired private WorkspaceRepository workspaces;
  @Autowired private WorkspaceMemberRepository members;
  @Autowired private AuditEventRepository audits;
  @Autowired private SourceDocumentRepository sources;
  @Autowired private SourceVersionRepository sourceVersions;
  @Autowired private SourcePreviewRepository sourcePreviews;
  @Autowired private AsyncJobRepository jobs;
  @Autowired private DocumentTemplateRepository templates;
  @Autowired private DocumentTemplateVersionRepository versions;
  @Autowired private DocumentTemplateResourceRepository resources;
  @Autowired private DocumentConversionWarningRepository warnings;
  @Autowired private DocumentTemplateOperationRepository operations;
  @Autowired private ParsedContentRepository parsedContents;

  @MockitoBean private ObjectStorage storage;
  @MockitoBean private AiServiceClient ai;
  @MockitoBean private DocumentPreviewConverter previewConverter;

  @BeforeEach
  void cleanDatabase() {
    operations.deleteAll();
    warnings.deleteAll();
    resources.deleteAll();
    versions.deleteAll();
    parsedContents.deleteAll();
    templates.deleteAll();
    jobs.deleteAll();
    sourcePreviews.deleteAll();
    sourceVersions.deleteAll();
    sources.deleteAll();
    audits.deleteAll();
    members.deleteAll();
    workspaces.deleteAll();
    users.deleteAll();
    reset(storage, ai, previewConverter);
  }

  @Test
  void convertsEditsPublishesAndRollsBackThroughImmutableVersions() {
    Fixture fixture = fixture();
    UUID requestId = UUID.randomUUID();
    var accepted =
        service.create(
            fixture.userId(),
            fixture.source().id(),
            new CreateTemplateRequest("合同模板"),
            "create-template",
            requestId);
    var job = jobs.findById(accepted.jobId()).orElseThrow();
    UUID resourceId = UUID.randomUUID();

    when(storage.open(fixture.source().objectBucket(), fixture.source().objectKey()))
        .thenReturn(new ByteArrayInputStream(PDF));
    when(storage.put(anyString(), anyString(), any(byte[].class), anyString()))
        .thenReturn("stored-etag");
    when(previewConverter.convert(eq(SourceFileType.PDF), any(byte[].class)))
        .thenReturn(new DocumentPreviewConverter.PreviewPdf(PDF, 1));
    when(ai.parse(
            eq(fixture.source().id()),
            eq("pdf"),
            eq("und"),
            eq("contract.pdf"),
            any(byte[].class),
            eq(requestId)))
        .thenReturn(parsed(fixture.source().id(), resourceId));

    handler.handle(AsyncJobCommand.from(job));

    var generated = service.get(fixture.userId(), accepted.templateId());
    assertThat(generated.template().conversionStatus()).isEqualTo("ready");
    assertThat(generated.currentVersion()).isNotNull();
    assertThat(generated.currentVersion().versionNumber()).isEqualTo(1);
    assertThat(generated.currentVersion().status()).isEqualTo("generated");
    assertThat(generated.currentVersion().resources()).hasSize(1);
    assertThat(generated.currentVersion().warnings()).hasSize(1);
    assertThat(generated.currentVersion().document().html())
        .contains("data-dm-node-id=\"paragraph-1\"")
        .contains("/api/v1/template-resources/" + resourceId + "/content");
    assertThat(
            sourcePreviews.findBySourceVersionId(fixture.source().id()).orElseThrow().status())
        .isEqualTo(SourcePreviewStatus.READY);
    assertThat(versions.findById(generated.currentVersion().id()).orElseThrow()
            .documentModelEnvelope().toString())
        .doesNotContain("合同正文");

    ObjectNode edited = (ObjectNode) generated.currentVersion().documentModel().deepCopy();
    ((ObjectNode) edited.at("/blocks/0/content/0")).put("text", "<script>修订合同</script>");
    String editKey = "edit-template";
    var editedVersion =
        service.createVersion(
            fixture.userId(),
            accepted.templateId(),
            new CreateTemplateVersionRequest(
                generated.currentVersion().id(), edited, "修订合同名称"),
            editKey,
            UUID.randomUUID());
    var editReplay =
        service.createVersion(
            fixture.userId(),
            accepted.templateId(),
            new CreateTemplateVersionRequest(
                generated.currentVersion().id(), edited, "修订合同名称"),
            editKey,
            UUID.randomUUID());

    assertThat(editedVersion.id()).isEqualTo(editReplay.id());
    assertThat(editedVersion.versionNumber()).isEqualTo(2);
    assertThat(editedVersion.status()).isEqualTo("checking");
    assertThat(editedVersion.diff().path("changes")).isNotEmpty();
    assertThat(editedVersion.document().html())
        .doesNotContain("<script>")
        .contains("&lt;script&gt;修订合同&lt;/script&gt;");
    assertThat(versions.count()).isEqualTo(2);

    var published =
        service.publish(
            fixture.userId(),
            accepted.templateId(),
            editedVersion.id(),
            new PublishTemplateVersionRequest("业务确认"),
            "publish-template",
            UUID.randomUUID());
    assertThat(published.status()).isEqualTo("published");

    var restored =
        service.rollback(
            fixture.userId(),
            accepted.templateId(),
            new RollbackTemplateRequest(generated.currentVersion().id(), "恢复自动生成版本"),
            "rollback-template",
            UUID.randomUUID());
    assertThat(restored.versionNumber()).isEqualTo(3);
    assertThat(restored.status()).isEqualTo("published");
    assertThat(restored.document().html()).contains("合同正文");
    assertThat(versions.findById(editedVersion.id()).orElseThrow().status())
        .isEqualTo(TemplateVersionStatus.SUPERSEDED);
    assertThat(service.get(fixture.userId(), accepted.templateId()).currentVersion().id())
        .isEqualTo(restored.id());
    assertThat(operations.count()).isEqualTo(3);
  }

  private Fixture fixture() {
    Instant now = Instant.parse("2026-08-01T00:00:00Z");
    UserAccount user = users.saveAndFlush(new UserAccount("template@example.com", "Template", "hash"));
    Workspace workspace =
        workspaces.saveAndFlush(
            new Workspace(
                "Template", "template-" + UUID.randomUUID(), user.id(), "seed", "0".repeat(64)));
    members.saveAndFlush(new WorkspaceMember(workspace, user, WorkspaceRole.OWNER));
    UUID sourceDocumentId = UUID.randomUUID();
    UUID sourceVersionId = UUID.randomUUID();
    sources.saveAndFlush(
        new SourceDocument(sourceDocumentId, workspace.id(), "Contract", user.id(), now));
    SourceVersion source =
        new SourceVersion(
            sourceVersionId,
            sourceDocumentId,
            workspace.id(),
            1,
            "contract.pdf",
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
    sourcePreviews.saveAndFlush(new SourcePreview(UUID.randomUUID(), source.id(), now));
    return new Fixture(user.id(), source);
  }

  private ParseDocumentResponse parsed(UUID sourceVersionId, UUID resourceId) {
    ObjectNode resource = objectMapper.createObjectNode();
    resource.put("id", resourceId.toString());
    resource.put("filename", "seal.png");
    resource.put("media_type", "image/png");
    resource.put("byte_length", IMAGE.length);
    resource.put("sha256", sha256(IMAGE));
    resource.put("content_base64", Base64.getEncoder().encodeToString(IMAGE));
    ObjectNode warning = objectMapper.createObjectNode();
    warning.put("severity", "warning");
    warning.put("code", "FONT_SUBSTITUTED");
    warning.put("message", "原字体不可用，已使用安全替代字体");
    warning.put("node_id", "paragraph-1");
    warning.put("page_number", 1);
    return new ParseDocumentResponse(
        sourceVersionId,
        "pdf",
        "docmind-parser/1.0",
        controlledDocument(resourceId),
        List.of(new ParsedTextNode("paragraph-1", "paragraph", 1, "合同正文", Map.of())),
        List.of(resource),
        List.of(warning));
  }

  private JsonNode controlledDocument(UUID resourceId) {
    return objectMapper.valueToTree(
        Map.of(
            "model_version", "1.0",
            "root_id", "document-root",
            "template_schema_version_id", objectMapper.nullNode(),
            "metadata", Map.of("title", "合同", "language", "zh-CN", "source_page_count", 1),
            "page_layout",
                Map.of(
                    "size", "a4",
                    "orientation", "portrait",
                    "width", length(210),
                    "height", length(297),
                    "margins", Map.of("top", length(25.4), "right", length(25.4), "bottom", length(25.4), "left", length(25.4)),
                    "header_distance", length(12.7),
                    "footer_distance", length(12.7)),
            "headers", List.of(),
            "footers", List.of(),
            "blocks",
                List.of(
                    Map.of(
                        "id", "paragraph-1",
                        "type", "paragraph",
                        "source", Map.of("source_node_id", "paragraph-1", "page_number", 1),
                        "attributes", Map.of(),
                        "style", Map.of("alignment", "left"),
                        "content",
                            List.of(
                                Map.of(
                                    "id", "text-1",
                                    "type", "text",
                                    "source", Map.of("source_node_id", "text-1", "page_number", 1),
                                    "attributes", Map.of(),
                                    "text", "合同正文",
                                    "style", Map.of()))),
                    Map.of(
                        "id", "image-1",
                        "type", "image",
                        "source", Map.of("source_node_id", "image-1", "page_number", 1),
                        "attributes", Map.of(),
                        "resource_id", resourceId.toString(),
                        "alt_text", "印章",
                        "title", "印章",
                        "width", length(24),
                        "height", length(24),
                        "alignment", "right"))));
  }

  private Map<String, Object> length(double value) {
    return Map.of("value", value, "unit", "mm");
  }

  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private record Fixture(UUID userId, SourceVersion source) {}
}
