package com.docmind.api.identity.api;

import com.docmind.api.identity.application.AuthService;
import com.docmind.api.identity.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

  private final AuthService authService;
  private final CurrentUser currentUser;

  public AuthController(AuthService authService, CurrentUser currentUser) {
    this.authService = authService;
    this.currentUser = currentUser;
  }

  @PostMapping("/auth/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.authenticate(request);
  }

  @GetMapping("/me")
  public UserSummaryResponse me() {
    return authService.currentUser(currentUser.id());
  }
}
