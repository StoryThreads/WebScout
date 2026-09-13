package com.webscout.service;

import com.webscout.entity.RefreshToken;

public record RefreshTokenResult(
        String rawToken,
        RefreshToken refreshToken
) {
}