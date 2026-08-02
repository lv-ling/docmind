package com.docmind.api.shared.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class RequestHashSupport {

  private RequestHashSupport() {}

  public static String sha256(ObjectMapper objectMapper, Object value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] canonicalJson =
          objectMapper
              .writer()
              .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
              .writeValueAsBytes(value);
      return HexFormat.of().formatHex(digest.digest(canonicalJson));
    } catch (NoSuchAlgorithmException | JsonProcessingException exception) {
      throw new IllegalStateException("request hashing failed", exception);
    }
  }
}
