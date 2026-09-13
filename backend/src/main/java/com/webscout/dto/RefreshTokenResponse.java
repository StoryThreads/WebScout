package com.webscout.dto;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken
) {}