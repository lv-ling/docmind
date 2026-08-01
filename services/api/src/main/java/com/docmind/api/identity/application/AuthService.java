package com.docmind.api.identity.application;

import com.docmind.api.identity.api.LoginRequest;
import com.docmind.api.identity.api.LoginResponse;
import com.docmind.api.identity.api.UserSummaryResponse;
import com.docmind.api.identity.domain.UserAccount;
import com.docmind.api.identity.domain.UserStatus;
import com.docmind.api.identity.infrastructure.UserAccountRepository;
import com.docmind.api.identity.security.AccessTokenService;
import com.docmind.api.shared.error.ApiErrorCategory;
import com.docmind.api.shared.error.ApiErrorCode;
import com.docmind.api.shared.error.ApiException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserAccountRepository users;
  private final PasswordEncoder passwordEncoder;
  private final AccessTokenService tokens;
  private final String dummyPasswordHash;

  public AuthService(
      UserAccountRepository users, PasswordEncoder passwordEncoder, AccessTokenService tokens) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.tokens = tokens;
    this.dummyPasswordHash = passwordEncoder.encode("docmind-dummy-password");
  }

  @Transactional(readOnly = true)
  public LoginResponse authenticate(LoginRequest request) {
    String normalizedEmail = request.email().strip().toLowerCase(Locale.ROOT);
    UserAccount user = users.findByEmail(normalizedEmail).orElse(null);
    String passwordHash = user == null ? dummyPasswordHash : user.passwordHash();
    boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);

    if (user == null || !passwordMatches || user.status() != UserStatus.ACTIVE) {
      throw invalidCredentials();
    }

    return new LoginResponse(
        tokens.issue(user), "Bearer", tokens.expiresInSeconds(), UserSummaryResponse.from(user));
  }

  @Transactional(readOnly = true)
  public UserSummaryResponse currentUser(UUID userId) {
    UserAccount user =
        users
            .findById(userId)
            .filter(candidate -> candidate.status() == UserStatus.ACTIVE)
            .orElseThrow(this::invalidCredentials);
    return UserSummaryResponse.from(user);
  }

  private ApiException invalidCredentials() {
    return new ApiException(
        HttpStatus.UNAUTHORIZED,
        ApiErrorCode.AUTHENTICATION_REQUIRED,
        ApiErrorCategory.AUTHENTICATION,
        "邮箱或密码不正确");
  }
}
