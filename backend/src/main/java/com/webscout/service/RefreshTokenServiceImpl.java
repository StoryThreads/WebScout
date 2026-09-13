package com.webscout.service;

import com.webscout.entity.RefreshToken;
import com.webscout.entity.User;
import com.webscout.exception.InvalidRefreshTokenException;
import com.webscout.exception.RefreshTokenReuseException;
import com.webscout.repository.RefreshTokenRepository;
import com.webscout.security.RefreshTokenGenerator;
import com.webscout.security.TokenHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public RefreshTokenResult rotate(String rawToken) {

        String tokenHash = tokenHasher.hash(rawToken);

        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        OffsetDateTime now = OffsetDateTime.now();

        if (currentToken.getRevokedAt() != null) {
            throw new RefreshTokenReuseException();
        }

        if (!currentToken.getExpiresAt().isAfter(now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = currentToken.getUser();

        String newRawToken = refreshTokenGenerator.generate();
        String newTokenHash = tokenHasher.hash(newRawToken);

        RefreshToken newToken = new RefreshToken(
                user,
                newTokenHash,
                now.plusDays(REFRESH_TOKEN_LIFETIME_DAYS),
                now
        );

        RefreshToken savedNewToken =
                refreshTokenRepository.save(newToken);

        currentToken.setRevokedAt(now);
        currentToken.setReplacedByToken(savedNewToken);

        refreshTokenRepository.save(currentToken);

        return new RefreshTokenResult(newRawToken, savedNewToken);
    }

    @Override
    @Transactional
    public void revoke(String rawToken, Long userId) {
        String tokenHash = tokenHasher.hash(rawToken);

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        // The authenticated user can only revoke their own refresh token.
        if (!refreshToken.getUser().getId().equals(userId)) {
            throw new InvalidRefreshTokenException();
        }

        // Idempotent logout for an already-revoked token.
        if (refreshToken.getRevokedAt() != null) {
            return;
        }

        refreshToken.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(refreshToken);
    }

}