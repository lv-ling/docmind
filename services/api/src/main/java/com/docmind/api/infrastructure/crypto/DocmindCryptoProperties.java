package com.docmind.api.infrastructure.crypto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "docmind.crypto")
public record DocmindCryptoProperties(
    @NotBlank String keyId,
    @NotBlank String masterKeyBase64) {

  @AssertTrue(message = "master-key-base64 must decode to exactly 32 bytes")
  public boolean isMasterKeyValid() {
    try {
      return Base64.getDecoder().decode(masterKeyBase64).length == 32;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  @Override
  public String toString() {
    return "DocmindCryptoProperties[keyId=" + keyId + ", masterKeyBase64=[REDACTED]]";
  }
}
