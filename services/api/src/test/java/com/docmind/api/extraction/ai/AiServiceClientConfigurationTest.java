package com.docmind.api.extraction.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

class AiServiceClientConfigurationTest {

  @Test
  void forcesHttp11ForMultipartRequestsToUvicornCompatibleServices() throws IOException {
    AtomicReference<CapturedRequest> captured = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/internal/v1/documents/parse",
        exchange -> {
          captured.set(
              new CapturedRequest(
                  exchange.getProtocol(),
                  exchange.getRequestHeaders().getFirst("Upgrade"),
                  exchange.getRequestHeaders().getFirst("HTTP2-Settings"),
                  exchange.getRequestHeaders().getFirst("X-DocMind-Internal-Token"),
                  exchange.getRequestHeaders().getFirst("Content-Type"),
                  new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1)));
          byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    try {
      String token = "test-internal-token-with-at-least-32-characters";
      var properties =
          new AiServiceProperties(
              true,
              "http://127.0.0.1:" + server.getAddress().getPort(),
              token,
              Duration.ofSeconds(2),
              Duration.ofSeconds(5),
              3,
              Duration.ofSeconds(30));
      RestClient client =
          new AiServiceClientConfiguration()
              .aiServiceRestClient(RestClient.builder(), properties);
      MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
      parts.add("source_version_id", "11111111-1111-4111-8111-111111111111");
      parts.add("source_format", "docx");
      parts.add("language", "und");
      parts.add(
          "file",
          new ByteArrayResource(new byte[] {'P', 'K', 3, 4}) {
            @Override
            public String getFilename() {
              return "fixture.docx";
            }
          });

      String response =
          client
              .post()
              .uri("/internal/v1/documents/parse")
              .contentType(MediaType.MULTIPART_FORM_DATA)
              .body(parts)
              .retrieve()
              .body(String.class);

      assertThat(response).isEqualTo("{}");
      CapturedRequest request = captured.get();
      assertThat(request).isNotNull();
      assertThat(request.protocol()).isEqualTo("HTTP/1.1");
      assertThat(request.upgrade()).isNull();
      assertThat(request.http2Settings()).isNull();
      assertThat(request.internalToken()).isEqualTo(token);
      assertThat(request.contentType()).startsWith("multipart/form-data;boundary=");
      assertThat(request.body())
          .contains("name=\"source_version_id\"")
          .contains("11111111-1111-4111-8111-111111111111")
          .contains("name=\"source_format\"")
          .contains("docx")
          .contains("name=\"language\"")
          .contains("und")
          .contains("name=\"file\"; filename=\"fixture.docx\"");
    } finally {
      server.stop(0);
    }
  }

  private record CapturedRequest(
      String protocol,
      String upgrade,
      String http2Settings,
      String internalToken,
      String contentType,
      String body) {}
}
