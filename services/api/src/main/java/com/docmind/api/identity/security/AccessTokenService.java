package com.docmind.api.identity.security;

import com.docmind.api.identity.domain.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenService {

  private final JwtEncoder encoder;
  private final AuthProperties properties;
  private final Clock clock;

  public AccessTokenService(JwtEncoder encoder, AuthProperties properties, Clock clock) {
    this.encoder = encoder;
    this.properties = properties;
    this.clock = clock;
  }

  public String issue(UserAccount user) {
    Instant issuedAt = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .subject(user.id().toString())
            .audience(java.util.List.of(properties.audience()))
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(properties.accessTokenTtl()))
            .id(UUID.randomUUID().toString())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  public long expiresInSeconds() {
    return properties.accessTokenTtl().toSeconds();
  }
}
