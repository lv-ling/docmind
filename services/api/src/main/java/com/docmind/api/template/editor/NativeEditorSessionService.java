package com.docmind.api.template.editor;

import com.docmind.api.identity.application.WorkspaceAccessService;
import com.docmind.api.identity.application.WorkspacePermission;
import com.docmind.api.identity.domain.WorkspaceMember;
import com.docmind.api.infrastructure.storage.DocmindStorageProperties;
import com.docmind.api.infrastructure.storage.ObjectStorage;
import com.docmind.api.infrastructure.storage.ObjectStorageException;
import io.minio.errors.ErrorResponseException;
import com.docmind.api.shared.error.ApiErrorCategory;
import com.docmind.api.shared.error.ApiErrorCode;
import com.docmind.api.shared.error.ApiException;
import com.docmind.api.source.application.SourceService;
import com.docmind.api.source.application.SourceService.BinaryContent;
import com.docmind.api.template.domain.DocumentTemplate;
import com.docmind.api.template.domain.TemplateConversionStatus;
import com.docmind.api.template.infrastructure.DocumentTemplateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "docmind.native-editor", name = "enabled", havingValue = "true")
public class NativeEditorSessionService {

  private static final String DOCX_MIME =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final int MAX_ZIP_ENTRIES = 10_000;
  private static final long MAX_EXPANDED_BYTES = 100L * 1024L * 1024L;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final DocumentTemplateRepository templates;
  private final WorkspaceAccessService access;
  private final SourceService sourceService;
  private final ObjectStorage storage;
  private final DocmindStorageProperties storageProperties;
  private final NativeEditorProperties properties;
  private final OnlyOfficeJwtService jwtService;
  private final Clock clock;
  private final HttpClient httpClient;
  private final Map<UUID, EditorSession> sessions = new ConcurrentHashMap<>();
  private final Map<UUID, SavedArtifact> latestByTemplate = new ConcurrentHashMap<>();

  public NativeEditorSessionService(
      DocumentTemplateRepository templates,
      WorkspaceAccessService access,
      SourceService sourceService,
      ObjectStorage storage,
      DocmindStorageProperties storageProperties,
      NativeEditorProperties properties,
      OnlyOfficeJwtService jwtService,
      Clock clock) {
    this.templates = templates;
    this.access = access;
    this.sourceService = sourceService;
    this.storage = storage;
    this.storageProperties = storageProperties;
    this.properties = properties;
    this.jwtService = jwtService;
    this.clock = clock;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
  }

  @Transactional(readOnly = true)
  public NativeEditorSessionResponse create(UUID userId, UUID templateId) {
    requireEnabled();
    DocumentTemplate template =
        templates.findById(templateId).orElseThrow(() -> notFound("模板不存在"));
    WorkspaceMember membership =
        access.require(userId, template.workspaceId(), WorkspacePermission.EDIT_CONTENT);
    if (template.conversionStatus() != TemplateConversionStatus.READY
        || template.currentVersionId() == null) {
      throw conflict("模板转换完成后才能进入原生编辑 POC");
    }

    BinaryContent original = sourceService.getOriginalContent(userId, template.sourceVersionId());
    if (!original.fileName().toLowerCase(Locale.ROOT).endsWith(".docx")) {
      throw validation("原生编辑 POC 当前仅接受 DOCX；DOC 将在正式接入阶段先转换为工作副本");
    }
    SavedArtifact latest = latestSavedArtifact(template);
    byte[] content = latest == null ? original.bytes() : latest.bytes();
    validateDocx(content);

    UUID sessionId = UUID.randomUUID();
    Instant now = clock.instant();
    Instant expiresAt = now.plus(properties.sessionTtl());
    String accessToken = randomToken();
    String documentKey = "docmind-" + sessionId.toString().replace("-", "");
    EditorSession session =
        new EditorSession(
            sessionId,
            templateId,
            template.workspaceId(),
            userId,
            documentKey,
            accessToken,
            original.fileName(),
            content,
            now,
            expiresAt);
    sessions.put(sessionId, session);

    String contentUrl =
        properties.normalizedApplicationInternalUrl()
            + "/api/v1/template-editor-sessions/"
            + sessionId
            + "/content?access_token="
            + accessToken;
    String callbackUrl =
        properties.normalizedApplicationInternalUrl()
            + "/api/v1/integrations/onlyoffice/callback/"
            + sessionId
            + "?access_token="
            + accessToken;

    Map<String, Object> permissions = new LinkedHashMap<>();
    permissions.put("comment", true);
    permissions.put("copy", true);
    permissions.put("download", false);
    permissions.put("edit", true);
    permissions.put("modifyContentControl", true);
    permissions.put("print", false);
    permissions.put("review", true);

    Map<String, Object> document = new LinkedHashMap<>();
    document.put("fileType", "docx");
    document.put("key", documentKey);
    document.put("permissions", permissions);
    document.put("title", safeTitle(original.fileName()));
    document.put("url", contentUrl);

    Map<String, Object> customization = new LinkedHashMap<>();
    customization.put("autosave", true);
    customization.put("compactHeader", false);
    customization.put("forcesave", true);
    customization.put("help", false);

    Map<String, Object> editorConfig = new LinkedHashMap<>();
    editorConfig.put("callbackUrl", callbackUrl);
    editorConfig.put("lang", "zh-CN");
    editorConfig.put("mode", "edit");
    editorConfig.put(
        "user",
        Map.of(
            "id", userId.toString(),
            "name", membership.user().displayName()));
    editorConfig.put("customization", customization);

    Map<String, Object> config = new LinkedHashMap<>();
    config.put("document", document);
    config.put("documentType", "word");
    config.put("editorConfig", editorConfig);
    config.put("height", "100%");
    config.put("type", "desktop");
    config.put("width", "100%");
    config.put("token", jwtService.sign(config));
    return new NativeEditorSessionResponse(
        sessionId, properties.normalizedDocumentServerPublicUrl(), Map.copyOf(config), expiresAt);
  }

  public NativeEditorBinaryContent content(UUID sessionId, String accessToken) {
    EditorSession session = requireSession(sessionId);
    requireAccessToken(session, accessToken);
    requireNotExpired(session, false);
    return new NativeEditorBinaryContent(session.content(), session.fileName(), DOCX_MIME);
  }

  public NativeEditorSessionStatusResponse status(UUID userId, UUID sessionId) {
    EditorSession session = requireSession(sessionId);
    if (!session.userId().equals(userId)) throw notFound("原生编辑会话不存在");
    return session.toResponse();
  }

  public void handleCallback(
      UUID sessionId, String accessToken, String authorizationHeader, JsonNode body) {
    EditorSession session = requireSession(sessionId);
    requireAccessToken(session, accessToken);
    requireNotExpired(session, true);
    verifyCallbackJwt(authorizationHeader, body);

    String key = body.path("key").asText("");
    int callbackStatus = body.path("status").asInt(-1);
    if (!session.documentKey().equals(key) || !Set.of(1, 2, 3, 4, 6, 7).contains(callbackStatus)) {
      throw forbiddenCallback();
    }

    synchronized (session) {
      session.callbackCount++;
      session.lastCallbackStatus = callbackStatus;
      if (callbackStatus == 1) {
        if (!session.hasSavedContent()) session.status = "editing";
        return;
      }
      if (callbackStatus == 4) {
        session.status = session.hasSavedContent() ? "closed_saved" : "closed_unchanged";
        return;
      }
      if (callbackStatus == 3 || callbackStatus == 7) {
        session.status = "save_error";
        return;
      }

      String downloadUrl = body.path("url").asText("");
      if (downloadUrl.isBlank()) throw forbiddenCallback();
      byte[] saved = downloadSavedFile(downloadUrl);
      validateDocx(saved);
      persistSavedArtifact(session, saved, callbackStatus);
    }
  }

  private void persistSavedArtifact(EditorSession session, byte[] bytes, int callbackStatus) {
    String sha256 = sha256(bytes);
    Instant savedAt = clock.instant();
    String baseKey = pocBaseKey(session.workspaceId(), session.templateId());
    String immutableKey = baseKey + "artifacts/" + sha256 + ".docx";
    String latestKey = baseKey + "latest.docx";
    try {
      storage.put(storageProperties.buckets().templates(), immutableKey, bytes, DOCX_MIME);
      storage.put(storageProperties.buckets().templates(), latestKey, bytes, DOCX_MIME);
    } catch (ObjectStorageException exception) {
      throw dependencyUnavailable("原生编辑保存文件暂时无法写入对象存储");
    }

    session.savedSha256 = sha256;
    session.savedSizeBytes = (long) bytes.length;
    session.savedAt = savedAt;
    session.content = bytes.clone();
    session.status = callbackStatus == 2 ? "saved_final" : "saved_forced";
    latestByTemplate.put(
        session.templateId(), new SavedArtifact(bytes.clone(), sha256, savedAt, latestKey));
  }

  private SavedArtifact latestSavedArtifact(DocumentTemplate template) {
    SavedArtifact cached = latestByTemplate.get(template.id());
    if (cached != null) return cached;

    String latestKey = pocBaseKey(template.workspaceId(), template.id()) + "latest.docx";
    String bucket = storageProperties.buckets().templates();
    ObjectStorage.StoredObject stored;
    try {
      stored = storage.stat(bucket, latestKey);
    } catch (ObjectStorageException exception) {
      if (isMissingObject(exception)) return null;
      throw dependencyUnavailable("无法读取原生编辑 POC 的最新保存文件");
    }
    // Mockito returns null for unstubbed collaborators; production implementations must not.
    if (stored == null) return null;
    if (stored.size() <= 0 || stored.size() > properties.maxFileSizeBytes()) {
      throw dependencyUnavailable("原生编辑 POC 的最新保存文件大小异常");
    }

    byte[] bytes;
    try (InputStream input = storage.open(bucket, latestKey)) {
      bytes = input.readNBytes(Math.toIntExact(properties.maxFileSizeBytes()) + 1);
    } catch (ObjectStorageException | IOException exception) {
      throw dependencyUnavailable("无法读取原生编辑 POC 的最新保存文件");
    }
    if (bytes.length != stored.size() || bytes.length > properties.maxFileSizeBytes()) {
      throw dependencyUnavailable("原生编辑 POC 的最新保存文件大小不一致");
    }
    validateDocx(bytes);
    SavedArtifact loaded =
        new SavedArtifact(bytes.clone(), sha256(bytes), clock.instant(), latestKey);
    SavedArtifact existing = latestByTemplate.putIfAbsent(template.id(), loaded);
    return existing == null ? loaded : existing;
  }

  private boolean isMissingObject(ObjectStorageException exception) {
    Throwable cause = exception.getCause();
    while (cause != null) {
      if (cause instanceof ErrorResponseException responseException) {
        String code = responseException.errorResponse().code();
        return "NoSuchKey".equals(code) || "NoSuchObject".equals(code);
      }
      cause = cause.getCause();
    }
    return false;
  }

  private String pocBaseKey(UUID workspaceId, UUID templateId) {
    return "native-editor-poc/" + workspaceId + "/" + templateId + "/";
  }

  private byte[] downloadSavedFile(String value) {
    URI supplied;
    try {
      supplied = URI.create(value);
    } catch (IllegalArgumentException exception) {
      throw forbiddenCallback();
    }
    if (supplied.getHost() == null
        || supplied.getUserInfo() != null
        || supplied.getFragment() != null
        || !allowedCallbackOrigin(supplied)) {
      throw forbiddenCallback();
    }

    URI downloadBase = URI.create(properties.normalizedDocumentServerDownloadUrl());
    URI target;
    try {
      target =
          new URI(
              downloadBase.getScheme(),
              null,
              downloadBase.getHost(),
              downloadBase.getPort(),
              supplied.getRawPath(),
              supplied.getRawQuery(),
              null);
    } catch (Exception exception) {
      throw forbiddenCallback();
    }

    HttpRequest request =
        HttpRequest.newBuilder(target)
            .GET()
            .timeout(Duration.ofSeconds(30))
            .header("Accept", DOCX_MIME)
            .build();
    try {
      HttpResponse<InputStream> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() != 200) {
        closeQuietly(response.body());
        throw dependencyUnavailable("编辑服务未能提供保存文件");
      }
      long declaredLength =
          response.headers().firstValueAsLong("Content-Length").orElse(-1L);
      if (declaredLength > properties.maxFileSizeBytes()) {
        closeQuietly(response.body());
        throw fileTooLarge();
      }
      try (InputStream input = response.body()) {
        byte[] bytes = input.readNBytes(Math.toIntExact(properties.maxFileSizeBytes()) + 1);
        if (bytes.length > properties.maxFileSizeBytes()) throw fileTooLarge();
        return bytes;
      }
    } catch (ApiException exception) {
      throw exception;
    } catch (IOException exception) {
      throw dependencyUnavailable("读取编辑服务保存文件失败");
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw dependencyUnavailable("读取编辑服务保存文件被中断");
    }
  }

  private boolean allowedCallbackOrigin(URI supplied) {
    String suppliedOrigin = origin(supplied);
    return properties.callbackAllowedOrigins().stream()
        .map(URI::create)
        .map(this::origin)
        .anyMatch(suppliedOrigin::equals);
  }

  private String origin(URI uri) {
    int port = uri.getPort();
    if (port < 0) port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    return uri.getScheme().toLowerCase(Locale.ROOT)
        + "://"
        + uri.getHost().toLowerCase(Locale.ROOT)
        + ":"
        + port;
  }

  private void verifyCallbackJwt(String authorizationHeader, JsonNode body) {
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw forbiddenCallback();
    }
    JsonNode signed;
    try {
      signed = jwtService.verify(authorizationHeader.substring("Bearer ".length()).strip());
    } catch (SecurityException exception) {
      throw forbiddenCallback();
    }
    JsonNode payload = signed.path("payload");
    if (!payload.isObject()
        || !payload.path("key").asText("").equals(body.path("key").asText(""))
        || payload.path("status").asInt(-1) != body.path("status").asInt(-2)) {
      throw forbiddenCallback();
    }
    if (body.hasNonNull("url")
        && !payload.path("url").asText("").equals(body.path("url").asText(""))) {
      throw forbiddenCallback();
    }
  }

  private void validateDocx(byte[] bytes) {
    if (bytes == null || bytes.length < 4 || bytes.length > properties.maxFileSizeBytes()) {
      throw fileTooLarge();
    }
    if (bytes[0] != 'P' || bytes[1] != 'K') throw validation("编辑结果不是有效的 DOCX 文件");

    boolean contentTypes = false;
    boolean documentXml = false;
    int entries = 0;
    long expanded = 0;
    byte[] buffer = new byte[8192];
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        entries++;
        if (entries > MAX_ZIP_ENTRIES) throw validation("DOCX 压缩包条目过多");
        String name = entry.getName().replace('\\', '/');
        String lower = name.toLowerCase(Locale.ROOT);
        if (name.startsWith("/") || name.contains("../")) {
          throw validation("DOCX 包含非法路径");
        }
        if (lower.endsWith("vbaproject.bin") || lower.endsWith("vbadata.xml")) {
          throw validation("原生模板不允许包含 VBA 宏");
        }
        if ("[Content_Types].xml".equals(name)) contentTypes = true;
        if ("word/document.xml".equals(name)) documentXml = true;
        int read;
        while ((read = zip.read(buffer)) != -1) {
          expanded += read;
          if (expanded > MAX_EXPANDED_BYTES) throw validation("DOCX 解压后内容超过安全限制");
        }
        zip.closeEntry();
      }
    } catch (ApiException exception) {
      throw exception;
    } catch (IOException exception) {
      throw validation("DOCX 压缩结构损坏");
    }
    if (!contentTypes || !documentXml) throw validation("DOCX 缺少必要的 OOXML 部件");
  }

  private String safeTitle(String fileName) {
    String value = fileName.replaceAll("[\\r\\n\\t]", " ").strip();
    return value.length() <= 200 ? value : value.substring(0, 200);
  }

  private String randomToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private void requireEnabled() {
    if (!properties.enabled()) {
      throw dependencyUnavailable("原生 DOCX 编辑 POC 未启用");
    }
  }

  private EditorSession requireSession(UUID sessionId) {
    EditorSession session = sessions.get(sessionId);
    if (session == null) throw notFound("原生编辑会话不存在");
    return session;
  }

  private void requireAccessToken(EditorSession session, String value) {
    byte[] expected = session.accessToken().getBytes(StandardCharsets.US_ASCII);
    byte[] actual = value == null ? new byte[0] : value.getBytes(StandardCharsets.US_ASCII);
    if (!MessageDigest.isEqual(expected, actual)) throw notFound("原生编辑会话不存在");
  }

  private void requireNotExpired(EditorSession session, boolean callbackGracePeriod) {
    Instant deadline =
        callbackGracePeriod ? session.expiresAt().plus(Duration.ofHours(1)) : session.expiresAt();
    if (clock.instant().isAfter(deadline)) {
      session.status = "expired";
      throw new ApiException(
          HttpStatus.GONE,
          ApiErrorCode.RESOURCE_CONFLICT,
          ApiErrorCategory.CONFLICT,
          "原生编辑会话已过期");
    }
  }

  private void closeQuietly(InputStream input) {
    try {
      input.close();
    } catch (IOException ignored) {
      // The response is already unusable; closing is best-effort.
    }
  }

  private ApiException notFound(String message) {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        ApiErrorCode.RESOURCE_NOT_FOUND,
        ApiErrorCategory.RESOURCE,
        message);
  }

  private ApiException validation(String message) {
    return new ApiException(
        HttpStatus.BAD_REQUEST,
        ApiErrorCode.VALIDATION_FAILED,
        ApiErrorCategory.VALIDATION,
        message);
  }

  private ApiException conflict(String message) {
    return new ApiException(
        HttpStatus.CONFLICT,
        ApiErrorCode.RESOURCE_CONFLICT,
        ApiErrorCategory.CONFLICT,
        message);
  }

  private ApiException forbiddenCallback() {
    return new ApiException(
        HttpStatus.FORBIDDEN,
        ApiErrorCode.PERMISSION_DENIED,
        ApiErrorCategory.AUTHORIZATION,
        "编辑服务回调校验失败");
  }

  private ApiException dependencyUnavailable(String message) {
    return new ApiException(
        HttpStatus.SERVICE_UNAVAILABLE,
        ApiErrorCode.DEPENDENCY_UNAVAILABLE,
        ApiErrorCategory.DEPENDENCY,
        message);
  }

  private ApiException fileTooLarge() {
    return new ApiException(
        HttpStatus.PAYLOAD_TOO_LARGE,
        ApiErrorCode.FILE_TOO_LARGE,
        ApiErrorCategory.VALIDATION,
        "原生编辑文件超过允许的大小");
  }

  private static final class EditorSession {
    private final UUID sessionId;
    private final UUID templateId;
    private final UUID workspaceId;
    private final UUID userId;
    private final String documentKey;
    private final String accessToken;
    private final String fileName;
    private final Instant createdAt;
    private final Instant expiresAt;
    private volatile byte[] content;
    private volatile String status = "created";
    private volatile Integer lastCallbackStatus;
    private volatile int callbackCount;
    private volatile String savedSha256;
    private volatile Long savedSizeBytes;
    private volatile Instant savedAt;

    private EditorSession(
        UUID sessionId,
        UUID templateId,
        UUID workspaceId,
        UUID userId,
        String documentKey,
        String accessToken,
        String fileName,
        byte[] content,
        Instant createdAt,
        Instant expiresAt) {
      this.sessionId = sessionId;
      this.templateId = templateId;
      this.workspaceId = workspaceId;
      this.userId = userId;
      this.documentKey = documentKey;
      this.accessToken = accessToken;
      this.fileName = fileName;
      this.content = content.clone();
      this.createdAt = createdAt;
      this.expiresAt = expiresAt;
    }

    private UUID templateId() { return templateId; }
    private UUID workspaceId() { return workspaceId; }
    private UUID userId() { return userId; }
    private String documentKey() { return documentKey; }
    private String accessToken() { return accessToken; }
    private String fileName() { return fileName; }
    private Instant expiresAt() { return expiresAt; }
    private byte[] content() { return content.clone(); }
    private boolean hasSavedContent() { return savedSha256 != null; }

    private NativeEditorSessionStatusResponse toResponse() {
      return new NativeEditorSessionStatusResponse(
          sessionId,
          templateId,
          status,
          expiresAt,
          lastCallbackStatus,
          callbackCount,
          savedSha256,
          savedSizeBytes,
          savedAt);
    }
  }

  private record SavedArtifact(byte[] bytes, String sha256, Instant savedAt, String objectKey) {}
}
