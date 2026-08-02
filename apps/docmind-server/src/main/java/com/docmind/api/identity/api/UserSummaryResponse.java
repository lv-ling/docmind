package com.docmind.api.identity.api;

import com.docmind.api.identity.domain.UserAccount;
import com.docmind.api.identity.domain.UserStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UserSummaryResponse(
    UUID id, String email, String displayName, UserStatus status) {

  public static UserSummaryResponse from(UserAccount user) {
    return new UserSummaryResponse(user.id(), user.email(), user.displayName(), user.status());
  }
}
