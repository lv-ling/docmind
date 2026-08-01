package com.docmind.api.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonEnvelopeEncryptionTest {

  private static final String KEY = "ZG9jbWluZC1sb2NhbC1tYXN0ZXIta2V5LTIwMjYhISE=";
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JsonEnvelopeEncryption encryption =
      new JsonEnvelopeEncryption(objectMapper, new DocmindCryptoProperties("test-v1", KEY));

  @Test
  void encryptsWithAUniqueDataKeyAndBindsCiphertextToItsContext() throws Exception {
    JsonNode plaintext = objectMapper.readTree("{\"phone\":\"+86 13800138000\"}");

    JsonNode first = encryption.encrypt(plaintext, "run:one");
    JsonNode second = encryption.encrypt(plaintext, "run:one");

    assertThat(first.toString()).doesNotContain("13800138000");
    assertThat(first.path("encrypted_data")).isNotEqualTo(second.path("encrypted_data"));
    assertThat(first.path("wrapped_key")).isNotEqualTo(second.path("wrapped_key"));
    assertThat(encryption.decrypt(first, "run:one")).isEqualTo(plaintext);
    assertThatThrownBy(() -> encryption.decrypt(first, "run:two"))
        .isInstanceOf(JsonEnvelopeEncryptionException.class)
        .hasMessageNotContaining("13800138000");
  }
}
