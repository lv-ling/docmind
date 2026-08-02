package com.docmind.api.identity.security;

import com.docmind.api.shared.error.ApiErrorCategory;
import com.docmind.api.shared.error.ApiErrorCode;
import com.docmind.api.shared.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

  public UUID id() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw authenticationRequired();
    }

    String subject = authentication.getName();
    if (subject == null) {
      throw authenticationRequired();
    }
    try {
      return UUID.fromString(subject);
    } catch (IllegalArgumentException exception) {
      throw authenticationRequired();
    }
  }

  private ApiException authenticationRequired() {
    return new ApiException(
        HttpStatus.UNAUTHORIZED,
        ApiErrorCode.AUTHENTICATION_REQUIRED,
        ApiErrorCategory.AUTHENTICATION,
        "需要登录后继续");
  }
}
