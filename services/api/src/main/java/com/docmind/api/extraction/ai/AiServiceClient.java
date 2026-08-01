package com.docmind.api.extraction.ai;

import com.docmind.api.extraction.ai.AiServiceContracts.AiExtractionRequest;
import com.docmind.api.extraction.ai.AiServiceContracts.AiExtractionResponse;
import com.docmind.api.extraction.ai.AiServiceContracts.ParseDocumentResponse;
import com.docmind.api.extraction.ai.AiServiceContracts.SensitiveTokenizationRequest;
import com.docmind.api.extraction.ai.AiServiceContracts.SensitiveTokenizationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "docmind.ai.enabled", havingValue = "true", matchIfMissing = true)
public class AiServiceClient {

  private final RestClient restClient;
  private final AiCircuitBreaker circuitBreaker;
  private final ObjectMapper objectMapper;

  public AiServiceClient(
      RestClient aiServiceRestClient, AiCircuitBreaker circuitBreaker, ObjectMapper objectMapper) {
    this.restClient = aiServiceRestClient;
    this.circuitBreaker = circuitBreaker;
    this.objectMapper = objectMapper;
  }

  public ParseDocumentResponse parse(
      UUID sourceVersionId,
      String sourceFormat,
      String language,
      String filename,
      byte[] content,
      UUID requestId) {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("source_version_id", sourceVersionId.toString());
    parts.add("source_format", sourceFormat);
    parts.add("language", language);
    parts.add(
        "file",
        new ByteArrayResource(content) {
          @Override
          public String getFilename() {
            return filename;
          }
        });
    return invoke(
        () ->
            restClient
                .post()
                .uri("/internal/v1/documents/parse")
                .header("X-Request-ID", requestId.toString())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve()
                .body(ParseDocumentResponse.class));
  }

  public SensitiveTokenizationResponse tokenize(
      SensitiveTokenizationRequest request, UUID requestId) {
    return invoke(
        () ->
            restClient
                .post()
                .uri("/internal/v1/sensitive/tokenize")
                .header("X-Request-ID", requestId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(SensitiveTokenizationResponse.class));
  }

  public AiExtractionResponse extract(AiExtractionRequest request) {
    return invoke(
        () ->
            restClient
                .post()
                .uri("/internal/v1/extractions/run")
                .header("X-Request-ID", request.requestId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiExtractionResponse.class));
  }

  private <T> T invoke(RemoteCall<T> call) {
    circuitBreaker.acquirePermission();
    try {
      T response = call.execute();
      if (response == null) {
        throw new AiServiceClientException("AI_EMPTY_RESPONSE", true, null);
      }
      circuitBreaker.recordSuccess();
      return response;
    } catch (AiServiceClientException exception) {
      circuitBreaker.recordFailure(exception.retryable());
      throw exception;
    } catch (ResourceAccessException exception) {
      circuitBreaker.recordFailure(true);
      throw new AiServiceClientException("AI_SERVICE_UNAVAILABLE", true, exception);
    } catch (RestClientResponseException exception) {
      boolean retryable = isRetryable(exception.getStatusCode());
      circuitBreaker.recordFailure(retryable);
      throw new AiServiceClientException(
          retryable ? "AI_SERVICE_UNAVAILABLE" : rejectionFailureCode(exception),
          retryable,
          exception);
    } catch (RuntimeException exception) {
      circuitBreaker.recordFailure(true);
      throw new AiServiceClientException("AI_SERVICE_FAILURE", true, exception);
    }
  }

  private boolean isRetryable(HttpStatusCode status) {
    return status.is5xxServerError() || status.value() == 408 || status.value() == 429;
  }

  String rejectionFailureCode(RestClientResponseException exception) {
    try {
      String reason =
          objectMapper.readTree(exception.getResponseBodyAsByteArray()).path("details").path("reason").asText();
      return reason.isBlank() ? "AI_REQUEST_REJECTED" : reason;
    } catch (IOException exceptionIgnored) {
      return "AI_REQUEST_REJECTED";
    }
  }

  @FunctionalInterface
  private interface RemoteCall<T> {
    T execute();
  }
}
