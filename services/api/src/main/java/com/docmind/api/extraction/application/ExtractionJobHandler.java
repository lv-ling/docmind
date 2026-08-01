package com.docmind.api.extraction.application;

import com.docmind.api.extraction.ai.AiServiceClient;
import com.docmind.api.extraction.ai.AiServiceClientException;
import com.docmind.api.extraction.ai.AiServiceContracts.AiExtractionRequest;
import com.docmind.api.extraction.ai.AiServiceContracts.AiExtractionResponse;
import com.docmind.api.extraction.ai.AiServiceContracts.ParseDocumentResponse;
import com.docmind.api.extraction.ai.AiServiceContracts.SensitiveTokenizationRequest;
import com.docmind.api.extraction.ai.AiServiceContracts.SensitiveTokenizationResponse;
import com.docmind.api.extraction.domain.AsyncJobType;
import com.docmind.api.extraction.messaging.AsyncJobCommand;
import com.docmind.api.extraction.messaging.AsyncJobExecutionException;
import com.docmind.api.extraction.messaging.AsyncJobHandler;
import com.docmind.api.infrastructure.storage.ObjectStorage;
import com.docmind.api.infrastructure.storage.ObjectStorageException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "docmind.ai.enabled", havingValue = "true", matchIfMissing = true)
public class ExtractionJobHandler implements AsyncJobHandler {

  private static final long ABSOLUTE_MAX_SOURCE_BYTES = 50L * 1024 * 1024;

  private final ExtractionExecutionStateService states;
  private final ObjectStorage objectStorage;
  private final AiServiceClient ai;
  private final AiExtractionRequestFactory requests;
  private final SensitiveTokenMappingService mappings;
  private final ExtractionResultValidator validator;
  private final ExtractionResultPersistenceService persistence;

  public ExtractionJobHandler(
      ExtractionExecutionStateService states,
      ObjectStorage objectStorage,
      AiServiceClient ai,
      AiExtractionRequestFactory requests,
      SensitiveTokenMappingService mappings,
      ExtractionResultValidator validator,
      ExtractionResultPersistenceService persistence) {
    this.states = states;
    this.objectStorage = objectStorage;
    this.ai = ai;
    this.requests = requests;
    this.mappings = mappings;
    this.validator = validator;
    this.persistence = persistence;
  }

  @Override
  public AsyncJobType jobType() {
    return AsyncJobType.EXTRACTION;
  }

  @Override
  public void handle(AsyncJobCommand command) {
    try {
      Optional<ExtractionWorkItem> workItem = states.start(command);
      if (workItem.isEmpty()) {
        return;
      }
      ExtractionWorkItem item = workItem.orElseThrow();
      byte[] sourceBytes = readSource(item);
      ParseDocumentResponse parsed =
          ai.parse(
              item.source().id(),
              item.source().fileType().wireValue(),
              "und",
              item.source().originalFileName(),
              sourceBytes,
              command.requestId());
      SensitiveTokenizationRequest tokenizationRequest =
          requests.tokenizationRequest(item, parsed);
      SensitiveTokenizationResponse tokenized =
          ai.tokenize(tokenizationRequest, command.requestId());
      SensitiveTokenMapping mapping =
          mappings.build(item.run().id(), item.source().id(), parsed, tokenized);
      mappings.replacePersisted(item.run().id(), mapping);
      AiExtractionRequest extractionRequest =
          requests.extractionRequest(item, parsed, tokenized, command.requestId());
      AiExtractionResponse response = ai.extract(extractionRequest);
      List<String> validationErrors =
          validator.validate(extractionRequest, response, mapping.replacements());
      persistence.persist(item, response, mapping, validationErrors);
    } catch (AiServiceClientException exception) {
      throw new AsyncJobExecutionException(
          exception.failureCode(), exception.retryable(), exception);
    } catch (ExtractionResultValidationException exception) {
      throw new AsyncJobExecutionException(exception.failureCode(), false, exception);
    } catch (ExtractionExecutionStateException exception) {
      throw new AsyncJobExecutionException(exception.failureCode(), false, exception);
    } catch (ObjectStorageException exception) {
      throw new AsyncJobExecutionException("SOURCE_STORAGE_UNAVAILABLE", true, exception);
    } catch (IOException exception) {
      throw new AsyncJobExecutionException("SOURCE_READ_FAILED", true, exception);
    }
  }

  @Override
  public void onRetryScheduled(AsyncJobCommand command, String failureCode) {
    states.markRetrying(command.aggregateId(), failureCode);
  }

  @Override
  public void onTerminalFailure(AsyncJobCommand command, String failureCode) {
    states.markFailed(command.aggregateId(), failureCode);
  }

  private byte[] readSource(ExtractionWorkItem item) throws IOException {
    Long declaredSize = item.source().sizeBytes();
    if (declaredSize == null || declaredSize <= 0 || declaredSize > ABSOLUTE_MAX_SOURCE_BYTES) {
      throw new ExtractionResultValidationException("SOURCE_SIZE_INVALID");
    }
    try (InputStream input =
        objectStorage.open(item.source().objectBucket(), item.source().objectKey())) {
      byte[] bytes = input.readNBytes(Math.toIntExact(declaredSize + 1));
      if (bytes.length != declaredSize.intValue()) {
        throw new ExtractionResultValidationException("SOURCE_SIZE_MISMATCH");
      }
      if (item.source().sha256() == null
          || !sha256(bytes).equalsIgnoreCase(item.source().sha256())) {
        throw new ExtractionResultValidationException("SOURCE_DIGEST_MISMATCH");
      }
      return bytes;
    }
  }

  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
