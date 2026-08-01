package com.docmind.api.infrastructure.crypto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class JsonEnvelopeEncryption {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final String WIRE_ALGORITHM = "A256GCM";
  private static final int IV_BYTES = 12;
  private static final int TAG_BITS = 128;

  private final ObjectMapper objectMapper;
  private final String keyId;
  private final SecretKey masterKey;
  private final SecureRandom secureRandom = new SecureRandom();

  public JsonEnvelopeEncryption(ObjectMapper objectMapper, DocmindCryptoProperties properties) {
    this.objectMapper = objectMapper;
    this.keyId = properties.keyId();
    this.masterKey =
        new SecretKeySpec(Base64.getDecoder().decode(properties.masterKeyBase64()), "AES");
  }

  public ObjectNode encrypt(JsonNode plaintext, String context) {
    requireContext(context);
    try {
      SecretKey dataKey = generateDataKey();
      byte[] dataIv = randomIv();
      byte[] wrappedKeyIv = randomIv();
      byte[] encryptedData =
          crypt(
              Cipher.ENCRYPT_MODE,
              dataKey,
              dataIv,
              dataAad(context),
              objectMapper.writeValueAsBytes(plaintext));
      byte[] wrappedKey =
          crypt(
              Cipher.ENCRYPT_MODE,
              masterKey,
              wrappedKeyIv,
              keyAad(),
              dataKey.getEncoded());

      ObjectNode envelope = objectMapper.createObjectNode();
      envelope.put("version", 1);
      envelope.put("algorithm", WIRE_ALGORITHM);
      envelope.put("key_id", keyId);
      envelope.put("data_iv", encode(dataIv));
      envelope.put("encrypted_data", encode(encryptedData));
      envelope.put("wrapped_key_iv", encode(wrappedKeyIv));
      envelope.put("wrapped_key", encode(wrappedKey));
      return envelope;
    } catch (GeneralSecurityException | JsonProcessingException exception) {
      throw new JsonEnvelopeEncryptionException("Unable to encrypt protected JSON", exception);
    }
  }

  public JsonNode decrypt(JsonNode envelope, String context) {
    requireContext(context);
    requireEnvelope(envelope);
    try {
      byte[] dataKeyBytes =
          crypt(
              Cipher.DECRYPT_MODE,
              masterKey,
              decode(envelope, "wrapped_key_iv"),
              keyAad(),
              decode(envelope, "wrapped_key"));
      SecretKey dataKey = new SecretKeySpec(dataKeyBytes, "AES");
      byte[] plaintext =
          crypt(
              Cipher.DECRYPT_MODE,
              dataKey,
              decode(envelope, "data_iv"),
              dataAad(context),
              decode(envelope, "encrypted_data"));
      return objectMapper.readTree(plaintext);
    } catch (GeneralSecurityException | IOException | IllegalArgumentException exception) {
      throw new JsonEnvelopeEncryptionException("Unable to decrypt protected JSON", exception);
    }
  }

  private SecretKey generateDataKey() throws GeneralSecurityException {
    KeyGenerator generator = KeyGenerator.getInstance("AES");
    generator.init(256, secureRandom);
    return generator.generateKey();
  }

  private byte[] randomIv() {
    byte[] iv = new byte[IV_BYTES];
    secureRandom.nextBytes(iv);
    return iv;
  }

  private byte[] crypt(
      int mode, SecretKey key, byte[] iv, byte[] aad, byte[] input)
      throws GeneralSecurityException {
    if (iv.length != IV_BYTES) {
      throw new GeneralSecurityException("Invalid GCM IV length");
    }
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));
    cipher.updateAAD(aad);
    return cipher.doFinal(input);
  }

  private void requireEnvelope(JsonNode envelope) {
    if (envelope == null
        || !envelope.isObject()
        || envelope.path("version").asInt(-1) != 1
        || !WIRE_ALGORITHM.equals(envelope.path("algorithm").asText())
        || !keyId.equals(envelope.path("key_id").asText())) {
      throw new JsonEnvelopeEncryptionException("Unsupported protected JSON envelope", null);
    }
  }

  private byte[] decode(JsonNode envelope, String field) {
    String value = envelope.path(field).asText(null);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing protected JSON envelope field");
    }
    return Base64.getDecoder().decode(value);
  }

  private String encode(byte[] value) {
    return Base64.getEncoder().encodeToString(value);
  }

  private byte[] dataAad(String context) {
    return ("docmind:data:v1:" + context).getBytes(StandardCharsets.UTF_8);
  }

  private byte[] keyAad() {
    return ("docmind:key:v1:" + keyId).getBytes(StandardCharsets.UTF_8);
  }

  private void requireContext(String context) {
    if (context == null || context.isBlank() || context.length() > 500) {
      throw new IllegalArgumentException("Protected JSON context is invalid");
    }
  }
}
