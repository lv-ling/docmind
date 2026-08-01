package com.docmind.api.identity.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LoginResponse(
    String accessToken, String tokenType, long expiresIn, UserSummaryResponse user) {}
