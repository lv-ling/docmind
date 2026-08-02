package com.docmind.api.extraction.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

class AiServiceClientTest {

  @Test
  void preservesSafeWorkflowReasonFromRejectedAiResponse() {
    AiServiceClient client =
        new AiServiceClient(RestClient.builder().build(), circuitBreaker(), new ObjectMapper());
    HttpClientErrorException response =
        HttpClientErrorException.create(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Unprocessable Entity",
            HttpHeaders.EMPTY,
            "{\"details\":{\"reason\":\"MODEL_PII_LEAK_DETECTED\"}}"
                .getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8);

    assertThat(client.rejectionFailureCode(response)).isEqualTo("MODEL_PII_LEAK_DETECTED");
  }

  @Test
  void fallsBackWhenValidationResponseHasNoWorkflowReason() {
    AiServiceClient client =
        new AiServiceClient(RestClient.builder().build(), circuitBreaker(), new ObjectMapper());
    HttpClientErrorException response =
        HttpClientErrorException.create(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Unprocessable Entity",
            HttpHeaders.EMPTY,
            "{\"detail\":[]}".getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8);

    assertThat(client.rejectionFailureCode(response)).isEqualTo("AI_REQUEST_REJECTED");
  }

  private AiCircuitBreaker circuitBreaker() {
    return new AiCircuitBreaker(3, Duration.ofSeconds(1), Clock.systemUTC());
  }
}
