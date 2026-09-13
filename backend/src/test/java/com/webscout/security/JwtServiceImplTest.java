package com.webscout.security;

import com.webscout.entity.User;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceImplTest {

    @Test
    void generateAccessToken_shouldContainExpectedClaimsAndValidSignature() {
        JwtProperties properties = new JwtProperties();

        String secret = "dPIfiLAObiLOYcH3nEABCRF9wQMxIWHHWr0g00fs6Qc=";

        properties.setSecret(secret);
        properties.setAccessTokenLifetime(Duration.ofMinutes(15));

        JwtServiceImpl jwtService = new JwtServiceImpl(properties);

        User user = new User(
                "test@example.com",
                "unused",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        String token = jwtService.generateAccessToken(user);

        var claims = Jwts.parser()
                .verifyWith(properties.signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(String.valueOf(user.getId()), claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getId());

        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void extractUserId_shouldReturnUserIdForValidToken() {
        JwtProperties properties = new JwtProperties();

        String secret = "dPIfiLAObiLOYcH3nEABCRF9wQMxIWHHWr0g00fs6Qc=";

        properties.setSecret(secret);
        properties.setAccessTokenLifetime(Duration.ofMinutes(15));

        JwtServiceImpl jwtService = new JwtServiceImpl(properties);

        User user = new User(
                "test@example.com",
                "unused",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        user.setId(42L);

        String token = jwtService.generateAccessToken(user);

        Long userId = jwtService.extractUserId(token);

        assertEquals(42L, userId);
    }

    @Test
    void extractUserId_shouldRejectTamperedToken() {
        JwtProperties properties = new JwtProperties();

        String secret = "dPIfiLAObiLOYcH3nEABCRF9wQMxIWHHWr0g00fs6Qc=";

        properties.setSecret(secret);
        properties.setAccessTokenLifetime(Duration.ofMinutes(15));

        JwtServiceImpl jwtService = new JwtServiceImpl(properties);

        User user = new User(
                "test@example.com",
                "unused",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        user.setId(42L);

        String token = jwtService.generateAccessToken(user);

        String tamperedToken = token.substring(0, token.length() - 1) + "x";

        assertThrows(
                Exception.class,
                () -> jwtService.extractUserId(tamperedToken)
        );
    }

    @Test
    void extractUserId_shouldRejectExpiredToken() {
        JwtProperties properties = new JwtProperties();

        String secret = "dPIfiLAObiLOYcH3nEABCRF9wQMxIWHHWr0g00fs6Qc=";

        properties.setSecret(secret);
        properties.setAccessTokenLifetime(Duration.ofMinutes(15));

        JwtServiceImpl jwtService = new JwtServiceImpl(properties);

        User user = new User(
                "test@example.com",
                "unused",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        user.setId(42L);

        OffsetDateTime now = OffsetDateTime.now();

        String expiredToken = Jwts.builder()
                .subject("42")
                .issuedAt(Date.from(now.minusMinutes(20).toInstant()))
                .expiration(Date.from(now.minusMinutes(10).toInstant()))
                .id(UUID.randomUUID().toString())
                .signWith(properties.signingKey())
                .compact();

        assertThrows(
                Exception.class,
                () -> jwtService.extractUserId(expiredToken)
        );
    }
}