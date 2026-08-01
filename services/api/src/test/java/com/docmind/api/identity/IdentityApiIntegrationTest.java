package com.docmind.api.identity;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.docmind.api.audit.infrastructure.AuditEventRepository;
import com.docmind.api.identity.domain.UserAccount;
import com.docmind.api.identity.domain.Workspace;
import com.docmind.api.identity.domain.WorkspaceMember;
import com.docmind.api.identity.domain.WorkspaceRole;
import com.docmind.api.identity.infrastructure.UserAccountRepository;
import com.docmind.api.identity.infrastructure.WorkspaceMemberRepository;
import com.docmind.api.identity.infrastructure.WorkspaceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class IdentityApiIntegrationTest {

  private static final String UUID_PATTERN =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
  private static final String PASSWORD = "SecurePass123!";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private UserAccountRepository users;
  @Autowired private WorkspaceRepository workspaces;
  @Autowired private WorkspaceMemberRepository members;
  @Autowired private AuditEventRepository audits;

  @BeforeEach
  void cleanDatabase() {
    audits.deleteAll();
    members.deleteAll();
    workspaces.deleteAll();
    users.deleteAll();
  }

  @Test
  void protectsWorkspaceEndpointsWithTheContractErrorShape() throws Exception {
    mockMvc
        .perform(get("/api/v1/workspaces"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
        .andExpect(jsonPath("$.category").value("authentication"))
        .andExpect(jsonPath("$.request_id").value(matchesPattern(UUID_PATTERN)))
        .andExpect(header().string("X-Request-ID", matchesPattern(UUID_PATTERN)));
  }

  @Test
  void authenticatesWithJwtAndReturnsTheCurrentUser() throws Exception {
    UserAccount user = createUser("owner@example.com", "Owner");
    String token = login(user.email(), PASSWORD);

    mockMvc
        .perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(user.id().toString()))
        .andExpect(jsonPath("$.email").value("owner@example.com"))
        .andExpect(jsonPath("$.display_name").value("Owner"))
        .andExpect(jsonPath("$.status").value("active"));
  }

  @Test
  void createsAWorkspaceIdempotentlyAndRecordsOneAuditEvent() throws Exception {
    UserAccount owner = createUser("owner@example.com", "Owner");
    String token = login(owner.email(), PASSWORD);
    String idempotencyKey = UUID.randomUUID().toString();
    String request = "{\"name\":\"Legal Team\",\"slug\":\"legal-team\"}";

    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/workspaces")
                    .header("Authorization", "Bearer " + token)
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(APPLICATION_JSON)
                    .content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("owner"))
            .andReturn();
    String workspaceId =
        objectMapper.readTree(created.getResponse().getContentAsByteArray()).get("id").asText();

    mockMvc
        .perform(
            post("/api/v1/workspaces")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(APPLICATION_JSON)
                .content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(workspaceId));

    mockMvc
        .perform(
            post("/api/v1/workspaces")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(APPLICATION_JSON)
                .content("{\"name\":\"Other Team\",\"slug\":\"other-team\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

    mockMvc
        .perform(get("/api/v1/workspaces").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(workspaceId))
        .andExpect(jsonPath("$[0].role").value("owner"));

    org.assertj.core.api.Assertions.assertThat(audits.count()).isEqualTo(1);
  }

  @Test
  void enforcesObjectLevelMemberPermissions() throws Exception {
    UserAccount owner = createUser("owner@example.com", "Owner");
    UserAccount viewer = createUser("viewer@example.com", "Viewer");
    Workspace workspace =
        workspaces.saveAndFlush(
            new Workspace(
                "Legal Team", "legal-team", owner.id(), "test-seed", "0".repeat(64)));
    members.saveAndFlush(new WorkspaceMember(workspace, owner, WorkspaceRole.OWNER));
    members.saveAndFlush(new WorkspaceMember(workspace, viewer, WorkspaceRole.VIEWER));

    String viewerToken = login(viewer.email(), PASSWORD);
    mockMvc
        .perform(
            get("/api/v1/workspaces/{workspaceId}/members", workspace.id())
                .header("Authorization", "Bearer " + viewerToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));

    String ownerToken = login(owner.email(), PASSWORD);
    mockMvc
        .perform(
            get("/api/v1/workspaces/{workspaceId}/members", workspace.id())
                .header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].role").value("owner"))
        .andExpect(jsonPath("$[1].role").value("viewer"));
  }

  @Test
  void returnsTheSameSafeMessageForUnknownUsersAndWrongPasswords() throws Exception {
    createUser("owner@example.com", "Owner");

    assertInvalidLogin("missing@example.com", PASSWORD);
    assertInvalidLogin("owner@example.com", "WrongPass123!");
  }

  private UserAccount createUser(String email, String displayName) {
    return users.saveAndFlush(
        new UserAccount(email, displayName, passwordEncoder.encode(PASSWORD)));
  }

  private String login(String email, String password) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            java.util.Map.of("email", email, "password", password))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.expires_in").value(300))
            .andReturn();
    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
    return response.get("access_token").asText();
  }

  private void assertInvalidLogin(String email, String password) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        java.util.Map.of("email", email, "password", password))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
        .andExpect(jsonPath("$.message").value("邮箱或密码不正确"));
  }
}
