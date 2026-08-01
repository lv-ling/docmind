package com.docmind.api.template.editor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "docmind.native-editor", name = "enabled", havingValue = "true")
public class OnlyOfficeJwtService {

  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
  private static final byte[] HEADER =
      "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8);

  private final ObjectMapper objectMapper;
  private final byte[] secret;

  public OnlyOfficeJwtService(ObjectMapper objectMapper, NativeEditorProperties properties) {
    this.objectMapper = objectMapper;
    this.secret = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
  }

  public String sign(Map<String, Object> payload) {
    try {
      String encodedHeader = ENCODER.encodeToString(HEADER);
      String encodedPayload = ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
      String input = encodedHeader + "." + encodedPayload;
      return input + "." + ENCODER.encodeToString(hmac(input));
    } catch (Exception exception) {
      throw new IllegalStateException("ONLYOFFICE JWT signing failed", exception);
    }
  }

  public JsonNode verify(String token) {
    if (token == null || token.isBlank()) throw new SecurityException("Missing callback JWT");
    String[] parts = token.split("\\.", -1);
    if (parts.length != 3) throw new SecurityException("Malformed callback JWT");
    try {
      JsonNode header = objectMapper.readTree(DECODER.decode(parts[0]));
      if (!"HS256".equals(header.path("alg").asText())) {
        throw new SecurityException("Unsupported callback JWT algorithm");
      }
      byte[] expected = hmac(parts[0] + "." + parts[1]);
      byte[] actual = DECODER.decode(parts[2]);
      if (!MessageDigest.isEqual(expected, actual)) {
        throw new SecurityException("Invalid callback JWT signature");
      }
      return objectMapper.readTree(DECODER.decode(parts[1]));
    } catch (SecurityException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new SecurityException("Malformed callback JWT", exception);
    }
  }

  private byte[] hmac(String input) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret, "HmacSHA256"));
    return mac.doFinal(input.getBytes(StandardCharsets.US_ASCII));
  }
}
