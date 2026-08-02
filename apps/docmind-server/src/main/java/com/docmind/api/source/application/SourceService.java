package com.docmind.api.source.application;

import com.docmind.api.audit.application.AuditRecorder;
import com.docmind.api.audit.domain.AuditOutcome;
import com.docmind.api.identity.application.WorkspaceAccessService;
import com.docmind.api.identity.application.WorkspacePermission;
import com.docmind.api.infrastructure.storage.DocmindStorageProperties;
import com.docmind.api.infrastructure.storage.ObjectStorage;
import com.docmind.api.infrastructure.storage.ObjectStorage.StoredObject;
import com.docmind.api.infrastructure.storage.ObjectStorageException;
import com.docmind.api.shared.error.ApiErrorCategory;
import com.docmind.api.shared.error.ApiErrorCode;
import com.docmind.api.shared.error.ApiException;
import com.docmind.api.source.api.CompleteSourceUploadRequest;
import com.docmind.api.source.api.CompleteSourceUploadResponse;
import com.docmind.api.source.api.CreateSourceUploadRequest;
import com.docmind.api.source.api.CreateSourceUploadResponse;
import com.docmind.api.source.api.CreateSourceVersionUploadRequest;
import com.docmind.api.source.api.SourceDocumentDetailResponse;
import com.docmind.api.source.api.SourceDocumentPageResponse;
import com.docmind.api.source.api.SourceDocumentResponse;
import com.docmind.api.source.api.SourcePreviewAccessResponse;
import com.docmind.api.source.api.SourcePreviewResponse;
import com.docmind.api.source.api.SourceVersionResponse;
import com.docmind.api.source.api.UploadSessionResponse;
import com.docmind.api.source.domain.SourceDocument;
import com.docmind.api.source.domain.SourceFileType;
import com.docmind.api.source.domain.SourcePreview;
import com.docmind.api.source.domain.SourcePreviewStatus;
import com.docmind.api.source.domain.SourceUploadSession;
import com.docmind.api.source.domain.SourceVersion;
import com.docmind.api.source.domain.SourceVersionStatus;
import com.docmind.api.source.domain.UploadSessionStatus;
import com.docmind.api.source.infrastructure.SourceDocumentRepository;
import com.docmind.api.source.infrastructure.SourcePreviewRepository;
import com.docmind.api.source.infrastructure.SourceUploadSessionRepository;
import com.docmind.api.source.infrastructure.SourceVersionRepository;
import com.docmind.api.extraction.domain.AsyncJob;
import com.docmind.api.extraction.domain.AsyncJobType;
import com.docmind.api.extraction.infrastructure.AsyncJobRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SourceService {

  private static final Logger log = LoggerFactory.getLogger(SourceService.class);
  private static final int DEFAULT_LIMIT = 20;
  private static final int MAX_LIMIT = 100;
  private static final int MAX_PREVIEW_BYTES = 50 * 1024 * 1024;

  private final SourceDocumentRepository sources;
  private final SourceVersionRepository versions;
  private final SourceUploadSessionRepository uploads;
  private final SourcePreviewRepository previews;
  private final AsyncJobRepository jobs;
  private final WorkspaceAccessService access;
  private final ObjectStorage storage;
  private final DocmindStorageProperties storageProperties;
  private final SourceFileValidator fileValidator;
  private final AuditRecorder audit;
  private final Clock clock;

  public SourceService(
      SourceDocumentRepository sources,
      SourceVersionRepository versions,
      SourceUploadSessionRepository uploads,
      SourcePreviewRepository previews,
      AsyncJobRepository jobs,
      WorkspaceAccessService access,
      ObjectStorage storage,
      DocmindStorageProperties storageProperties,
      SourceFileValidator fileValidator,
      AuditRecorder audit,
      Clock clock) {
    this.sources = sources;
    this.versions = versions;
    this.uploads = uploads;
    this.previews = previews;
    this.jobs = jobs;
    this.access = access;
    this.storage = storage;
    this.storageProperties = storageProperties;
    this.fileValidator = fileValidator;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional
  public CreateUploadResult createSourceUpload(
      UUID userId,
      UUID workspaceId,
      CreateSourceUploadRequest request,
      String idempotencyKey,
      UUID requestId) {
    access.require(userId, workspaceId, WorkspacePermission.EDIT_CONTENT);
    String documentName = request.documentName().strip();
    SourceFileInput input =
        validateFileInput(
            request.originalFileName(), request.declaredMimeType(), request.sizeBytes());
    if (documentName.isBlank() || documentName.length() > 200) {
      throw validationError("文档名称长度必须为 1 到 200 个字符");
    }
    validateIdempotencyKey(idempotencyKey);
    String requestHash =
        DigestSupport.sha256(
            "create-source",
            documentName,
            input.originalFileName(),
            input.declaredMimeType(),
            Long.toString(input.sizeBytes()));

    SourceUploadSession existing =
        uploads
            .findByWorkspaceIdAndCreatedByAndCreationIdempotencyKey(
                workspaceId, userId, idempotencyKey)
            .orElse(null);
    if (existing != null) {
      return replayCreate(existing, requestHash);
    }

    Instant now = clock.instant();
    UUID sourceId = UUID.randomUUID();
    UUID versionId = UUID.randomUUID();
    SourceDocument source =
        sources.save(new SourceDocument(sourceId, workspaceId, documentName, userId, now));
    SourceVersion version =
        versions.save(
            new SourceVersion(
                versionId,
                sourceId,
                workspaceId,
                1,
                input.originalFileName(),
                input.fileType(),
                input.declaredMimeType(),
                input.sizeBytes(),
                storageProperties.buckets().sources(),
                stagingKey(workspaceId, sourceId, versionId, input.fileType()),
                storageProperties.buckets().sources(),
                objectKey(workspaceId, sourceId, versionId, input.fileType()),
                userId,
                now));
    SourceUploadSession upload =
        uploads.save(
            new SourceUploadSession(
                UUID.randomUUID(),
                workspaceId,
                sourceId,
                versionId,
                now.plus(storageProperties.uploadUrlTtl()),
                userId,
                idempotencyKey,
                requestHash,
                now));
    String uploadUrl = presignedPut(version, storageProperties.uploadUrlTtl());
    audit.record(
        workspaceId,
        userId,
        "source.upload_created",
        "source_version",
        versionId,
        AuditOutcome.SUCCESS,
        requestId,
        Map.of("version_number", 1, "file_type", input.fileType().wireValue()));
    return new CreateUploadResult(toCreateResponse(source, version, upload, uploadUrl), false);
  }

  @Transactional
  public CreateUploadResult createVersionUpload(
      UUID userId,
      UUID sourceId,
      CreateSourceVersionUploadRequest request,
      String idempotencyKey,
      UUID requestId) {
    SourceDocument source =
        sources.findLockedById(sourceId).orElseThrow(this::sourceNotFound);
    access.require(userId, source.workspaceId(), WorkspacePermission.EDIT_CONTENT);
    SourceFileInput input =
        validateFileInput(
            request.originalFileName(), request.declaredMimeType(), request.sizeBytes());
    validateIdempotencyKey(idempotencyKey);
    String requestHash =
        DigestSupport.sha256(
            "create-source-version",
            sourceId.toString(),
            input.originalFileName(),
            input.declaredMimeType(),
            Long.toString(input.sizeBytes()));

    SourceUploadSession existing =
        uploads
            .findByWorkspaceIdAndCreatedByAndCreationIdempotencyKey(
                source.workspaceId(), userId, idempotencyKey)
            .orElse(null);
    if (existing != null) {
      return replayCreate(existing, requestHash);
    }

    int versionNumber =
        versions
                .findTopBySourceDocumentIdOrderByVersionNumberDesc(sourceId)
                .map(SourceVersion::versionNumber)
                .orElse(0)
            + 1;
    Instant now = clock.instant();
    UUID versionId = UUID.randomUUID();
    SourceVersion version =
        versions.save(
            new SourceVersion(
                versionId,
                sourceId,
                source.workspaceId(),
                versionNumber,
                input.originalFileName(),
                input.fileType(),
                input.declaredMimeType(),
                input.sizeBytes(),
                storageProperties.buckets().sources(),
                stagingKey(source.workspaceId(), sourceId, versionId, input.fileType()),
                storageProperties.buckets().sources(),
                objectKey(source.workspaceId(), sourceId, versionId, input.fileType()),
                userId,
                now));
    SourceUploadSession upload =
        uploads.save(
            new SourceUploadSession(
                UUID.randomUUID(),
                source.workspaceId(),
                sourceId,
                versionId,
                now.plus(storageProperties.uploadUrlTtl()),
                userId,
                idempotencyKey,
                requestHash,
                now));
    String uploadUrl = presignedPut(version, storageProperties.uploadUrlTtl());
    audit.record(
        source.workspaceId(),
        userId,
        "source.version_upload_created",
        "source_version",
        versionId,
        AuditOutcome.SUCCESS,
        requestId,
        Map.of("version_number", versionNumber, "file_type", input.fileType().wireValue()));
    return new CreateUploadResult(toCreateResponse(source, version, upload, uploadUrl), false);
  }

  @Transactional(noRollbackFor = ApiException.class)
  public CompleteSourceUploadResponse completeUpload(
      UUID userId,
      UUID versionId,
      CompleteSourceUploadRequest request,
      String idempotencyKey,
      UUID requestId) {
    validateIdempotencyKey(idempotencyKey);
    SourceUploadSession upload =
        uploads
            .findLockedBySourceVersionId(versionId)
            .orElseThrow(() -> resourceNotFound("上传会话不存在"));
    SourceVersion version =
        versions.findById(versionId).orElseThrow(() -> resourceNotFound("来源版本不存在"));
    SourceDocument source =
        sources.findLockedById(version.sourceDocumentId()).orElseThrow(this::sourceNotFound);
    access.require(userId, version.workspaceId(), WorkspacePermission.EDIT_CONTENT);

    String normalizedEtag = normalizeEtag(request.objectEtag());
    String requestHash =
        DigestSupport.sha256(
            Long.toString(request.sizeBytes()),
            request.detectedMimeType(),
            request.sha256().toLowerCase(Locale.ROOT),
            normalizedEtag);
    if (upload.status() == UploadSessionStatus.COMPLETED) {
      verifyCompletionReplay(upload, idempotencyKey, requestHash);
      return new CompleteSourceUploadResponse(
          SourceDocumentResponse.from(source), SourceVersionResponse.from(version));
    }
    if (upload.status() == UploadSessionStatus.EXPIRED
        || upload.status() == UploadSessionStatus.ABORTED) {
      throw conflict("上传会话已失效，请创建新的版本上传会话");
    }
    Instant now = clock.instant();
    if (!now.isBefore(upload.expiresAt())) {
      upload.expire();
      scheduleStagingCleanup(version.uploadBucket(), version.uploadKey());
      throw conflict("上传会话已过期，请创建新的版本上传会话");
    }

    ValidatedObject object = validateStoredObject(version, request, normalizedEtag);
    String immutableEtag = promoteToImmutableObject(version, object.etag());
    version.complete(
        object.file().mimeType(),
        object.file().sizeBytes(),
        object.file().sha256(),
        immutableEtag,
        now);
    source.advanceTo(version.id(), userId, now);
    upload.complete(idempotencyKey, requestHash, now);
    UUID previewId = UUID.randomUUID();
    previews.save(new SourcePreview(previewId, version.id(), now));
    jobs.save(
        new AsyncJob(
            UUID.randomUUID(),
            version.workspaceId(),
            AsyncJobType.SOURCE_PREVIEW,
            "source_preview",
            previewId,
            3,
            requestId,
            userId,
            now));
    audit.record(
        version.workspaceId(),
        userId,
        "source.upload_completed",
        "source_version",
        version.id(),
        AuditOutcome.SUCCESS,
        requestId,
        Map.of(
            "version_number", version.versionNumber(),
            "file_type", version.fileType().wireValue(),
            "size_bytes", object.file().sizeBytes()));
    scheduleStagingCleanup(version.uploadBucket(), version.uploadKey());
    return new CompleteSourceUploadResponse(
        SourceDocumentResponse.from(source), SourceVersionResponse.from(version));
  }

  @Transactional(readOnly = true)
  public SourceDocumentPageResponse listSources(
      UUID userId, UUID workspaceId, String cursor, Integer requestedLimit) {
    access.require(userId, workspaceId, WorkspacePermission.VIEW_WORKSPACE);
    int limit = requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
    if (limit < 1 || limit > MAX_LIMIT) {
      throw validationError("limit 必须在 1 到 100 之间");
    }
    int pageIndex = decodeCursor(cursor, limit);
    Page<SourceDocument> page =
        sources.findAllByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            workspaceId, PageRequest.of(pageIndex, limit));
    List<SourceDocumentResponse> items =
        page.getContent().stream().map(SourceDocumentResponse::from).toList();
    String nextCursor = page.hasNext() ? encodeCursor(pageIndex + 1, limit) : null;
    return new SourceDocumentPageResponse(items, nextCursor, page.hasNext());
  }

  @Transactional(readOnly = true)
  public SourceDocumentDetailResponse getSource(UUID userId, UUID sourceId) {
    SourceDocument source = sources.findByIdAndDeletedAtIsNull(sourceId).orElseThrow(this::sourceNotFound);
    access.require(userId, source.workspaceId(), WorkspacePermission.VIEW_WORKSPACE);
    List<SourceVersionResponse> versionResponses =
        versions.findAllBySourceDocumentIdOrderByVersionNumberDesc(sourceId).stream()
            .map(SourceVersionResponse::from)
            .toList();
    return new SourceDocumentDetailResponse(SourceDocumentResponse.from(source), versionResponses);
  }

  @Transactional(readOnly = true)
  public SourcePreviewAccessResponse getPreview(UUID userId, UUID versionId) {
    SourceVersion version = requireVersion(userId, versionId);
    SourcePreview preview =
        previews
            .findBySourceVersionId(versionId)
            .orElseThrow(() -> resourceNotFound("预览尚未创建"));
    String viewUrl =
        preview.status() == SourcePreviewStatus.READY
            ? "/api/v1/source-previews/" + preview.id() + "/content"
            : null;
    return new SourcePreviewAccessResponse(
        SourcePreviewResponse.from(preview),
        viewUrl,
        "/api/v1/source-versions/" + version.id() + "/content");
  }

  @Transactional(readOnly = true)
  public BinaryContent getOriginalContent(UUID userId, UUID versionId) {
    SourceVersion version = requireVersion(userId, versionId);
    if (version.status() == SourceVersionStatus.UPLOADING) {
      throw conflict("来源文件尚未完成上传校验");
    }
    byte[] bytes = readObject(version.objectBucket(), version.objectKey(), (int) SourceFileValidator.MAX_FILE_SIZE_BYTES);
    return new BinaryContent(bytes, version.mimeType(), version.originalFileName(), version.objectEtag());
  }

  @Transactional(readOnly = true)
  public BinaryContent getPreviewContent(UUID userId, UUID previewId) {
    SourcePreview preview =
        previews.findById(previewId).orElseThrow(() -> resourceNotFound("预览不存在"));
    SourceVersion version = requireVersion(userId, preview.sourceVersionId());
    if (preview.status() != SourcePreviewStatus.READY
        || preview.objectBucket() == null
        || preview.objectKey() == null) {
      throw conflict("预览尚未就绪");
    }
    byte[] bytes = readObject(preview.objectBucket(), preview.objectKey(), MAX_PREVIEW_BYTES);
    return new BinaryContent(bytes, "application/pdf", version.originalFileName() + ".preview.pdf", null);
  }

  private CreateUploadResult replayCreate(SourceUploadSession upload, String requestHash) {
    if (!upload.creationRequestHash().equals(requestHash)) {
      throw idempotencyConflict();
    }
    SourceDocument source =
        sources.findByIdAndDeletedAtIsNull(upload.sourceDocumentId()).orElseThrow(this::sourceNotFound);
    SourceVersion version =
        versions
            .findById(upload.sourceVersionId())
            .orElseThrow(() -> resourceNotFound("来源版本不存在"));
    String uploadUrl = null;
    Instant now = clock.instant();
    Duration remaining = Duration.between(now, upload.expiresAt());
    if ((upload.status() == UploadSessionStatus.PENDING
            || upload.status() == UploadSessionStatus.UPLOADING)
        && !remaining.isNegative()
        && !remaining.isZero()
        && remaining.toSeconds() >= 1) {
      uploadUrl = presignedPut(version, remaining);
    } else if (upload.status() != UploadSessionStatus.COMPLETED) {
      upload.expire();
      scheduleStagingCleanup(version.uploadBucket(), version.uploadKey());
    }
    return new CreateUploadResult(toCreateResponse(source, version, upload, uploadUrl), true);
  }

  private ValidatedObject validateStoredObject(
      SourceVersion version, CompleteSourceUploadRequest request, String normalizedClientEtag) {
    StoredObject stored;
    try {
      stored = storage.stat(version.uploadBucket(), version.uploadKey());
    } catch (ObjectStorageException exception) {
      throw storageUnavailable();
    }
    if (stored.size() > SourceFileValidator.MAX_FILE_SIZE_BYTES) {
      throw new ApiException(
          HttpStatus.PAYLOAD_TOO_LARGE,
          ApiErrorCode.FILE_TOO_LARGE,
          ApiErrorCategory.VALIDATION,
          "文件不能超过 10 MiB");
    }
    if (stored.size() != version.expectedSizeBytes() || stored.size() != request.sizeBytes()) {
      throw validationError("文件大小与上传会话声明不一致");
    }
    String storedEtag = normalizeEtag(stored.etag());
    if (storedEtag.isBlank() || !storedEtag.equals(normalizedClientEtag)) {
      throw validationError("对象 ETag 与完成请求不一致");
    }
    byte[] bytes =
        readObject(
            version.uploadBucket(),
            version.uploadKey(),
            Math.toIntExact(SourceFileValidator.MAX_FILE_SIZE_BYTES));
    if (bytes.length != stored.size()) {
      throw validationError("对象大小在校验期间发生变化");
    }
    SourceFileValidator.ValidatedSourceFile file = fileValidator.validate(bytes, version.fileType());
    if (!file.mimeType().equals(request.detectedMimeType())) {
      throw validationError("文件 MIME 类型与完成请求不一致");
    }
    if (!file.sha256().equalsIgnoreCase(request.sha256())) {
      throw validationError("文件 SHA-256 与完成请求不一致");
    }
    return new ValidatedObject(file, storedEtag);
  }

  private byte[] readObject(String bucket, String objectKey, int maxBytes) {
    try (InputStream input = storage.open(bucket, objectKey)) {
      byte[] bytes = input.readNBytes(maxBytes + 1);
      if (bytes.length > maxBytes) {
        throw new ApiException(
            HttpStatus.PAYLOAD_TOO_LARGE,
            ApiErrorCode.FILE_TOO_LARGE,
            ApiErrorCategory.VALIDATION,
            "文件超过允许的大小");
      }
      return bytes;
    } catch (ApiException exception) {
      throw exception;
    } catch (IOException | ObjectStorageException exception) {
      throw storageUnavailable();
    }
  }

  private SourceVersion requireVersion(UUID userId, UUID versionId) {
    SourceVersion version =
        versions.findById(versionId).orElseThrow(() -> resourceNotFound("来源版本不存在"));
    access.require(userId, version.workspaceId(), WorkspacePermission.VIEW_WORKSPACE);
    return version;
  }

  private SourceFileInput validateFileInput(
      String originalFileName, String declaredMimeType, long sizeBytes) {
    String fileName = originalFileName.strip();
    if (fileName.isBlank()
        || fileName.length() > 255
        || fileName.contains("/")
        || fileName.contains("\\")
        || fileName.codePoints().anyMatch(character -> Character.isISOControl(character))) {
      throw validationError("原始文件名无效");
    }
    if (sizeBytes < 1) {
      throw validationError("文件不能为空");
    }
    if (sizeBytes > SourceFileValidator.MAX_FILE_SIZE_BYTES) {
      throw new ApiException(
          HttpStatus.PAYLOAD_TOO_LARGE,
          ApiErrorCode.FILE_TOO_LARGE,
          ApiErrorCategory.VALIDATION,
          "文件不能超过 10 MiB");
    }
    int extensionStart = fileName.lastIndexOf('.');
    String extension =
        extensionStart < 0 ? "" : fileName.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
    SourceFileType fileType =
        switch (extension) {
          case "doc" -> SourceFileType.DOC;
          case "docx" -> SourceFileType.DOCX;
          case "pdf" -> SourceFileType.PDF;
          default ->
              throw new ApiException(
                  HttpStatus.BAD_REQUEST,
                  ApiErrorCode.FILE_TYPE_NOT_ALLOWED,
                  ApiErrorCategory.VALIDATION,
                  "仅支持 DOC、DOCX 或 PDF 文件");
        };
    String mimeType = declaredMimeType.strip().toLowerCase(Locale.ROOT);
    if (!fileType.mimeType().equals(mimeType)) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          ApiErrorCode.FILE_TYPE_NOT_ALLOWED,
          ApiErrorCategory.VALIDATION,
          "声明的 MIME 类型与文件扩展名不匹配");
    }
    return new SourceFileInput(fileName, fileType, mimeType, sizeBytes);
  }

  private void verifyCompletionReplay(
      SourceUploadSession upload, String idempotencyKey, String requestHash) {
    if (!idempotencyKey.equals(upload.completionIdempotencyKey())
        || !requestHash.equals(upload.completionRequestHash())) {
      throw idempotencyConflict();
    }
  }

  private String presignedPut(SourceVersion version, Duration ttl) {
    try {
      return storage.presignedPut(version.uploadBucket(), version.uploadKey(), ttl);
    } catch (ObjectStorageException exception) {
      throw storageUnavailable();
    }
  }

  private CreateSourceUploadResponse toCreateResponse(
      SourceDocument source,
      SourceVersion version,
      SourceUploadSession upload,
      String uploadUrl) {
    return new CreateSourceUploadResponse(
        SourceDocumentResponse.from(source),
        SourceVersionResponse.from(version),
        UploadSessionResponse.from(upload, uploadUrl));
  }

  private String objectKey(
      UUID workspaceId, UUID sourceId, UUID versionId, SourceFileType fileType) {
    return "immutable/"
        + workspaceId
        + "/"
        + sourceId
        + "/"
        + versionId
        + "/original."
        + fileType.wireValue();
  }

  private String stagingKey(
      UUID workspaceId, UUID sourceId, UUID versionId, SourceFileType fileType) {
    return "staging/"
        + workspaceId
        + "/"
        + sourceId
        + "/"
        + versionId
        + "/upload."
        + fileType.wireValue();
  }

  private String promoteToImmutableObject(SourceVersion version, String sourceEtag) {
    try {
      String destinationEtag =
          storage.copyIfMatch(
              version.uploadBucket(),
              version.uploadKey(),
              sourceEtag,
              version.objectBucket(),
              version.objectKey());
      String normalized = normalizeEtag(destinationEtag);
      return normalized.isBlank() ? sourceEtag : normalized;
    } catch (ObjectStorageException exception) {
      throw storageUnavailable();
    }
  }

  private void scheduleStagingCleanup(String bucket, String objectKey) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              storage.delete(bucket, objectKey);
            } catch (ObjectStorageException exception) {
              log.warn(
                  "source_staging_cleanup_failed exception_type={}",
                  exception.getCause() == null
                      ? exception.getClass().getName()
                      : exception.getCause().getClass().getName());
            }
          }
        });
  }

  private int decodeCursor(String cursor, int limit) {
    if (cursor == null || cursor.isBlank()) {
      return 0;
    }
    try {
      String decoded =
          new String(Base64.getUrlDecoder().decode(cursor), java.nio.charset.StandardCharsets.UTF_8);
      String[] parts = decoded.split(":", -1);
      if (parts.length != 3 || !parts[0].equals("v1") || Integer.parseInt(parts[2]) != limit) {
        throw new IllegalArgumentException("cursor mismatch");
      }
      int page = Integer.parseInt(parts[1]);
      if (page < 0) {
        throw new IllegalArgumentException("negative cursor");
      }
      return page;
    } catch (IllegalArgumentException exception) {
      throw validationError("cursor 无效或与 limit 不匹配");
    }
  }

  private String encodeCursor(int page, int limit) {
    String value = "v1:" + page + ":" + limit;
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private String normalizeEtag(String etag) {
    if (etag == null) {
      return "";
    }
    String normalized = etag.strip();
    if (normalized.startsWith("W/")) {
      normalized = normalized.substring(2).strip();
    }
    if (normalized.length() >= 2
        && normalized.startsWith("\"")
        && normalized.endsWith("\"")) {
      normalized = normalized.substring(1, normalized.length() - 1);
    }
    return normalized;
  }

  private void validateIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey == null
        || idempotencyKey.isBlank()
        || idempotencyKey.length() > 128
        || idempotencyKey.codePoints().anyMatch(character -> Character.isISOControl(character))) {
      throw validationError("Idempotency-Key 长度必须为 1 到 128 个可见字符");
    }
  }

  private ApiException sourceNotFound() {
    return resourceNotFound("来源文档不存在");
  }

  private ApiException resourceNotFound(String message) {
    return new ApiException(
        HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, ApiErrorCategory.RESOURCE, message);
  }

  private ApiException validationError(String message) {
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

  private ApiException idempotencyConflict() {
    return new ApiException(
        HttpStatus.CONFLICT,
        ApiErrorCode.IDEMPOTENCY_CONFLICT,
        ApiErrorCategory.CONFLICT,
        "幂等键已用于不同的请求");
  }

  private ApiException storageUnavailable() {
    return new ApiException(
        HttpStatus.SERVICE_UNAVAILABLE,
        ApiErrorCode.DEPENDENCY_UNAVAILABLE,
        ApiErrorCategory.DEPENDENCY,
        "对象存储暂时不可用");
  }

  public record CreateUploadResult(CreateSourceUploadResponse response, boolean replayed) {}

  public record BinaryContent(byte[] bytes, String mimeType, String fileName, String etag) {}

  private record SourceFileInput(
      String originalFileName,
      SourceFileType fileType,
      String declaredMimeType,
      long sizeBytes) {}

  private record ValidatedObject(
      SourceFileValidator.ValidatedSourceFile file, String etag) {}
}
