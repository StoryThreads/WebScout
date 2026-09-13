package com.webscout.security;

import com.webscout.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceImplTest {

    private static final String SECRET =
            "dPIfiLAObiLOYcH3nEABCRF9wQMxIWHHWr0g00fs6Qc=";

    private User createUser(Long id) {
        User user = new User(
                "test@example.com",
                "unused",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        setUserId(user, id);

        return user;
    }

    private void setUserId(User user, Long id) {
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(
                    "Failed to assign test user ID",
                    e
            );
        }
    }

    private JwtServiceImpl createJwtService() {
        JwtProperties properties = new JwtProperties();

        properties.setSecret(SECRET);
        properties.setAccessTokenLifetime(
                Duration.ofMinutes(15)
        );

        return new JwtServiceImpl(properties);
    }

    private JwtProperties createJwtProperties() {
        JwtProperties properties = new JwtProperties();

        properties.setSecret(SECRET);
        properties.setAccessTokenLifetime(
                Duration.ofMinutes(15)
        );

        return properties;
    }

    @Test
    void generateAccessToken_shouldContainExpectedClaimsAndValidSignature() {

        JwtProperties properties = createJwtProperties();

        JwtServiceImpl jwtService =
                new JwtServiceImpl(properties);

        User user = createUser(42L);

        String token =
                jwtService.generateAccessToken(user);

        var claims = Jwts.parser()
                .verifyWith(properties.signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(
                "42",
                claims.getSubject()
        );

        assertNotNull(
                claims.getIssuedAt()
        );

        assertNotNull(
                claims.getExpiration()
        );

        assertNotNull(
                claims.getId()
        );

        assertTrue(
                claims.getExpiration()
                        .after(claims.getIssuedAt())
        );
    }

    @Test
    void extractUserId_shouldReturnUserIdForValidToken() {

        JwtServiceImpl jwtService =
                createJwtService();

        User user = createUser(42L);

        String token =
                jwtService.generateAccessToken(user);

        Long userId =
                jwtService.extractUserId(token);

        assertEquals(
                42L,
                userId
        );
    }

    @Test
    void extractUserId_shouldRejectTamperedToken() {

        JwtServiceImpl jwtService =
                createJwtService();

        User user = createUser(42L);

        String token =
                jwtService.generateAccessToken(user);

        String[] tokenParts = token.split("\\.");

        assertEquals(
                3,
                tokenParts.length
        );

        String signature = tokenParts[2];

        char firstCharacter = signature.charAt(0);

        char replacement =
                firstCharacter == 'A' ? 'B' : 'A';

        String tamperedSignature =
                replacement + signature.substring(1);

        String tamperedToken =
                tokenParts[0]
                        + "."
                        + tokenParts[1]
                        + "."
                        + tamperedSignature;

        assertThrows(
                JwtException.class,
                () -> jwtService.extractUserId(tamperedToken)
        );
    }

    @Test
    void extractUserId_shouldRejectExpiredToken() {

        JwtProperties properties =
                createJwtProperties();

        JwtServiceImpl jwtService =
                new JwtServiceImpl(properties);

        OffsetDateTime now =
                OffsetDateTime.now();

        String expiredToken = Jwts.builder()
                .subject("42")
                .issuedAt(
                        Date.from(
                                now.minusMinutes(20)
                                        .toInstant()
                        )
                )
                .expiration(
                        Date.from(
                                now.minusMinutes(10)
                                        .toInstant()
                        )
                )
                .id(UUID.randomUUID().toString())
                .signWith(properties.signingKey())
                .compact();

        assertThrows(
                JwtException.class,
                () -> jwtService.extractUserId(expiredToken)
        );
    }
}