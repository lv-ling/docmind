package com.docmind.api.identity.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, SecurityErrorWriter securityErrorWriter) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/template-editor-sessions/*/content",
                        "/api/v1/integrations/onlyoffice/callback/*",
                        "/actuator/health/**",
                        "/actuator/info")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            securityErrorWriter.authenticationRequired(request, response))
                    .accessDeniedHandler(
                        (request, response, exception) ->
                            securityErrorWriter.permissionDenied(request, response)))
        .oauth2ResourceServer(
            resourceServer ->
                resourceServer
                    .jwt(Customizer.withDefaults())
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            securityErrorWriter.authenticationRequired(request, response))
                    .accessDeniedHandler(
                        (request, response, exception) ->
                            securityErrorWriter.permissionDenied(request, response)));
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  JwtEncoder jwtEncoder(AuthProperties properties) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
  }

  @Bean
  JwtDecoder jwtDecoder(AuthProperties properties) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(secretKey(properties))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefaultWithIssuer(properties.issuer());
    OAuth2TokenValidator<Jwt> audience =
        new JwtClaimValidator<List<String>>(
            "aud", values -> values != null && values.contains(properties.audience()));
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults, audience));
    return decoder;
  }

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  private SecretKey secretKey(AuthProperties properties) {
    return new SecretKeySpec(
        properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }
}
