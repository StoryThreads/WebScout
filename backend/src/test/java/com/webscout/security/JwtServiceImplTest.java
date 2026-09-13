package com.webscout.security;

import com.webscout.entity.User;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;

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
}