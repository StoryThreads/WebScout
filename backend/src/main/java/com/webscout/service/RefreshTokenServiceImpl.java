package com.webscout.service;

import com.webscout.entity.RefreshToken;
import com.webscout.entity.User;
import com.webscout.repository.RefreshTokenRepository;
import com.webscout.security.RefreshTokenGenerator;
import com.webscout.security.TokenHasher;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final long REFRESH_TOKEN_LIFETIME_DAYS = 30;

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final TokenHasher tokenHasher;

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenGenerator refreshTokenGenerator,
            TokenHasher tokenHasher
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.tokenHasher = tokenHasher;
    }

    @Override
    public RefreshTokenResult create(User user) {
        String rawToken = refreshTokenGenerator.generate();

        String tokenHash = tokenHasher.hash(rawToken);

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusDays(REFRESH_TOKEN_LIFETIME_DAYS);

        RefreshToken refreshToken = new RefreshToken(
                user,
                tokenHash,
                expiresAt,
                now
        );

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);

        return new RefreshTokenResult(rawToken, savedToken);
    }
}