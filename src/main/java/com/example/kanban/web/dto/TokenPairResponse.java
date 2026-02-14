package com.example.kanban.web.dto;

public record TokenPairResponse(
    String tokenType,
    String accessToken,
    long accessTokenExpiresInSeconds,
    String refreshToken
) {
}
