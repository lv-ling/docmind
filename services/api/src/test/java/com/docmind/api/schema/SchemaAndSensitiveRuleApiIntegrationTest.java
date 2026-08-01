package com.docmind.api.schema;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

import com.docmind.api.audit.infrastructure.AuditEventRepository;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class SchemaAndSensitiveRuleApiIntegrationTest {

  private static final String PASSWORD = "SecurePass123!";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private UserAccountRepository users;
  @Autowired private WorkspaceRepository workspaces;
  @Autowired private WorkspaceMemberRepository members;
  @Autowired private AuditEventRepository audits;
  @Autowired private ExtractionSchemaRepository schemas;
  @Autowired private ExtractionSchemaVersionRepository schemaVersions;
  @Autowired private ExtractionSchemaFieldRepository schemaFields;
  @Autowired private SchemaTemplateRepository schemaTemplates;
  @Autowired private SensitiveRuleTemplateRepository sensitiveTemplates;
  @Autowired private SensitiveRuleTemplateVersionRepository sensitiveVersions;
  @Autowired private SensitiveRuleRepository sensitiveRules;

  @BeforeEach
  void cleanDatabase() {
    sensitiveRules.deleteAll();
    sensitiveVersions.deleteAll();
    sensitiveTemplates.deleteAll();
    schemaTemplates.deleteAll();
    schemaFields.deleteAll();
    schemaVersions.deleteAll();
    schemas.deleteAll();
    audits.deleteAll();
    members.deleteAll();
    workspaces.deleteAll();
    users.deleteAll();
  }

  @Test
  void createsAndVersionsSchemasWithLiteralDefaultSemantics() throws Exception {
    TestContext context = createContext(WorkspaceRole.OWNER);
    String firstRequest = schemaRequest("literal", "\"\"", "string", false);
    String idempotencyKey = UUID.randomUUID().toString();

    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/workspaces/{workspaceId}/schemas", context.workspaceId())
                    .header("Authorization", "Bearer " + context.token())
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(APPLICATION_JSON)
                    .content(firstRequest))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.current_version.version_number").value(1))
            .andExpect(jsonPath("$.current_version.status").value("published"))
            .andExpect(jsonPath("$.current_version.fields[0].default.kind").value("literal"))
            .andExpect(jsonPath("$.current_version.fields[0].default.value").value(""))
            .andExpect(
                jsonPath("$.current_version.json_schema.properties.customer.properties.name.default")
                    .value(""))
            .andExpect(jsonPath("$.current_version.json_schema.required[0]").value("customer"))
            .andExpect(
                jsonPath(
                        "$.current_version.json_schema.properties.customer.required[0]")
                    .value("name"))
            .andReturn();
    JsonNode createdBody = objectMapper.readTree(created.getResponse().getContentAsByteArray());
    String schemaId = createdBody.path("schema").path("id").asText();
    String firstVersionId = createdBody.path("current_version").path("id").asText();

    mockMvc
        .perform(
            post("/api/v1/workspaces/{workspaceId}/schemas", context.workspaceId())
                .header("Authorization", "Bearer " + context.token())
                .header("Idempotency-Key", idempotencyKey)
                .contentType(APPLICATION_JSON)
                .content(firstRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.schema.id").value(schemaId));

    mockMvc
        .perform(
            post("/api/v1/workspaces/{workspaceId}/schemas", context.workspaceId())
                .header("Authorization", "Bearer " + context.token())
                .header("Idempotency-Key", idempotencyKey)
                .contentType(APPLICATION_JSON)
                .content(firstRequest.replace("Customer schema", "Other schema")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

    String nextVersionRequest =
        """
        {
          "fields": [%s],
          "change_summary": "allow nullable missing values"
        }
        """
            .formatted(schemaField("none", null, "string", true));
    mockMvc
        .perform(
            post("/api/v1/schemas/{schemaId}/versions", schemaId)
                .header("Authorization", "Bearer " + context.token())
                .header("Idempotency-Key", idempotencyKey)
                .contentType(APPLICATION_JSON)
                .content(nextVersionRequest))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.version_number").value(2))
        .andExpect(jsonPath("$.fields[0].default.kind").value("none"))
        .andExpect(jsonPath("$.fields[0].default.value").doesNotExist())
        .andExpect(
            jsonPath(
                    "$.json_schema.properties.customer.properties.name['x-docmind-null-policy']")
                .value("missing_without_default"));

    mockMvc
        .perform(get("/api/v1/schemas/{schemaId}", schemaId).header("Authorization", "Bearer " + context.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versions[0].version_number").value(2))
        .andExpect(jsonPath("$.versions[0].status").value("published"))
        .andExpect(jsonPath("$.versions[1].version_number").value(1))
        .andExpect(jsonPath("$.versions[1].status").value("superseded"));

    mockMvc
        .perform(
            post("/api/v1/workspaces/{workspaceId}/schema-templates", context.workspaceId())
                .header("Authorization", "Bearer " + context.token())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "name", "Customer template",
                            "description", "Reusable v1 snapshot",
                            "schema_version_id", firstVersionId))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.current_schema_version_id").value(firstVersionId));

    org.assertj.core.api.Assertions.assertThat(audits.count()).isEqualTo(3);
  }

  @Test
  void rejectsExpressionDefaultsAndTypeMismatches() throws Exception {
    TestContext context = createContext(WorkspaceRole.OWNER);

    mockMvc
        .perform(
            post("/api/v1/workspaces/{workspaceId}/schemas", context.workspaceId())
                .header("Authorization", "Bearer " + context.token())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(APPLICATION_JSON)
                .content(schemaRequest("expression", "\"now()\"", "string", false)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SCHEMA_INVALID"))
        .andExpect(jsonPath("$.field_errors[0].path").value("fields[0].default.kind"));

    mockMvc
        .perform(
            post("/api/v1/workspaces/{workspaceId}/schemas", context.workspaceId())
                .header("Authorization", "Bearer " + context.token())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(APPLICATION_JSON)
                .content(schemaRequest("literal", "\"not-a-number\"", "number", false)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SCHEMA_INVALID"))
        .andExpect(jsonPath("$.field_errors[0].code").value("type_mismatch"));

    mockMvc
        .perform(
            post("/api/v1/workspaces/{workspaceId}/schemas", context.workspaceId())
                .header("Authorization", "Bearer " + context.token())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(APPLICATION_JSON)
                .content(schemaRequest("literal", "null", "string", true)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.current_version.fields[0].default.kind").value("literal"))
        .andExpect(jsonPath("$.current_version.fields[0].default.value").value(nullValue()));
  }

  @Test
  void createsRe2SafeSensitiveRuleVersionsAndRejectsBackreferences() throws Exception {
    TestContext context = createContext(WorkspaceRole.OWNER);
    String createRequest = sensitiveTemplateRequest("(?:\\+86)?1[3-9][0-9]{9}");
    MvcResult created =
        mockMvc
            .perform(
                post(
                        "/api/v1/workspaces/{workspaceId}/sensitive-rule-templates",
                        context.workspaceId())
                    .header("Authorization", "Bearer " + context.token())
                    .header("Idempotency-Key", UUID.randomUUID())
                    .contentType(APPLICATION_JSON)
                    .content(createRequest))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.current_version.rules[0].regex_dialect").value("re2"))
            .andExpect(jsonPath("$.current_version.rules[0].country_codes.length()").value(9))
            .andReturn();
    String templateId =
        objectMapper
            .readTree(created.getResponse().getContentAsByteArray())
            .path("template")
            .path("id")
            .asText();

    String versionRequest =
        """
        {
          "rules": [%s],
          "change_summary": "tighten phone pattern"
        }
        """
            .formatted(sensitiveRule("(?:\\+86)?1[3-9][0-9]{9}"));
    String versionKey = UUID.randomUUID().toString();
    mockMvc
        .perform(
            post("/api/v1/sensitive-rule-templates/{templateId}/versions", templateId)
                .header("Authorization", "Bearer " + context.token())
                .header("Idempotency-Key", versionKey)
                .contentType(APPLICATION_JSON)
                .content(versionRequest))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.version_number").value(2));
    mockMvc
        .perform(
            post("/api/v1/sensitive-rule-templates/{templateId}/versions", templateId)
                .header("Authorization", "Bearer " + context.token())
                .header("Idempotency-Key", versionKey)
                .contentType(APPLICATION_JSON)
                .content(versionRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version_number").value(2));

    mockMvc
        .perform(
            post(
                    "/api/v1/workspaces/{workspaceId}/sensitive-rule-templates",
                    context.workspaceId())
                .header("Authorization", "Bearer " + context.token())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(APPLICATION_JSON)
                .content(sensitiveTemplateRequest("([0-9]+)\\1")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.field_errors[0].code").value("invalid_re2"));
  }

  @Test
  void enforcesWorkspaceEditPermissionForSchemaCreation() throws Exception {
    TestContext context = createContext(WorkspaceRole.VIEWER);
    mockMvc
        .perform(
            post("/api/v1/workspaces/{workspaceId}/schemas", context.workspaceId())
                .header("Authorization", "Bearer " + context.token())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(APPLICATION_JSON)
                .content(schemaRequest("none", null, "string", true)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

    mockMvc
        .perform(
            get("/api/v1/workspaces/{workspaceId}/schemas", context.workspaceId())
                .header("Authorization", "Bearer " + context.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  private String schemaRequest(
      String defaultKind, String defaultValue, String valueType, boolean nullable) {
    return """
        {
          "name": "Customer schema",
          "description": "Customer extraction fields",
          "fields": [%s]
        }
        """
        .formatted(schemaField(defaultKind, defaultValue, valueType, nullable));
  }

  private String schemaField(
      String defaultKind, String defaultValue, String valueType, boolean nullable) {
    String valueProperty = defaultValue == null ? "" : ", \"value\": " + defaultValue;
    return """
        {
          "key": "customer_name",
          "json_path": "$.customer.name",
          "description": "Customer name",
          "value_type": "%s",
          "array_item_type": null,
          "required": true,
          "nullable": %s,
          "default": {"kind": "%s"%s},
          "sensitivity": "low",
          "constraints": {
            "format": null,
            "pattern": null,
            "enum_values": [],
            "min_length": null,
            "max_length": null,
            "minimum": null,
            "maximum": null
          },
          "examples": [],
          "extraction_hint": null,
          "display": {"mask": "partial", "view_role_keys": ["owner"]},
          "metadata": {},
          "position": 0
        }
        """
        .formatted(valueType, nullable, defaultKind, valueProperty);
  }

  private String sensitiveTemplateRequest(String pattern) {
    return """
        {
          "name": "International PII",
          "description": "Tenant phone rules",
          "rules": [%s]
        }
        """
        .formatted(sensitiveRule(pattern));
  }

  private String sensitiveRule(String pattern) {
    return """
        {
          "key": "international_phone",
          "name": "International phone",
          "description": "Phone number recognizer",
          "data_type": "phone_number",
          "recognizer_kind": "regex",
          "locales": ["zh-CN", "en-US"],
          "country_codes": ["CN", "US", "JP", "KR", "DE", "FR", "GB", "AU", "NL"],
          "regex_pattern": "%s",
          "regex_dialect": "re2",
          "dictionary_terms": [],
          "validator_name": null,
          "confidence_threshold": 0.85,
          "priority": 100,
          "enabled": true
        }
        """
        .formatted(pattern.replace("\\", "\\\\"));
  }

  private TestContext createContext(WorkspaceRole role) throws Exception {
    UserAccount owner =
        users.saveAndFlush(
            new UserAccount("owner@example.com", "Owner", passwordEncoder.encode(PASSWORD)));
    UserAccount actor = owner;
    Workspace workspace =
        workspaces.saveAndFlush(
            new Workspace("Legal Team", "legal-" + UUID.randomUUID(), owner.id(), "seed", "0".repeat(64)));
    members.saveAndFlush(new WorkspaceMember(workspace, owner, WorkspaceRole.OWNER));
    if (role != WorkspaceRole.OWNER) {
      actor =
          users.saveAndFlush(
              new UserAccount("actor@example.com", "Actor", passwordEncoder.encode(PASSWORD)));
      members.saveAndFlush(new WorkspaceMember(workspace, actor, role));
    }
    return new TestContext(workspace.id(), login(actor.email()), actor.id());
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

  private record TestContext(UUID workspaceId, String token, UUID userId) {}
}
