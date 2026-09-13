package com.webscout.service;

import com.webscout.entity.RefreshToken;
import com.webscout.entity.User;
import com.webscout.exception.InvalidRefreshTokenException;
import com.webscout.exception.RefreshTokenReuseException;
import com.webscout.repository.RefreshTokenRepository;
import com.webscout.security.RefreshTokenGenerator;
import com.webscout.security.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenServiceImplTest {

    private RefreshTokenRepository repository;
    private RefreshTokenGenerator generator;
    private TokenHasher hasher;

    private RefreshTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(RefreshTokenRepository.class);
        generator = mock(RefreshTokenGenerator.class);
        hasher = mock(TokenHasher.class);

        service = new RefreshTokenServiceImpl(
                repository,
                generator,
                hasher
        );
    }

    private void setUserId(User user, Long id) {
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to assign test user ID", e);
        }
    }

    @Test
    void shouldCreateRefreshToken() {

        User user = new User(
                "user@example.com",
                "hash",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        RefreshToken savedToken = mock(RefreshToken.class);

        when(generator.generate())
                .thenReturn("raw-token");

        when(hasher.hash("raw-token"))
                .thenReturn("token-hash");

        when(repository.save(any(RefreshToken.class)))
                .thenReturn(savedToken);

        RefreshTokenResult result =
                service.create(user);

        assertEquals("raw-token", result.rawToken());
        assertSame(savedToken, result.refreshToken());

        verify(generator).generate();
        verify(hasher).hash("raw-token");
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void shouldRejectUnknownRefreshToken() {

        when(hasher.hash("unknown-token"))
                .thenReturn("unknown-hash");

        when(repository.findByTokenHashForUpdate("unknown-hash"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.rotate("unknown-token")
        );
    }

    @Test
    void shouldRejectRevokedRefreshTokenAsReuse() {

        RefreshToken token = mock(RefreshToken.class);

        when(hasher.hash("old-token"))
                .thenReturn("old-hash");

        when(repository.findByTokenHashForUpdate("old-hash"))
                .thenReturn(Optional.of(token));

        when(token.getRevokedAt())
                .thenReturn(OffsetDateTime.now());

        assertThrows(
                RefreshTokenReuseException.class,
                () -> service.rotate("old-token")
        );

        verify(repository, never())
                .save(any());
    }

    @Test
    void shouldRejectExpiredRefreshToken() {

        RefreshToken token = mock(RefreshToken.class);

        when(hasher.hash("expired-token"))
                .thenReturn("expired-hash");

        when(repository.findByTokenHashForUpdate("expired-hash"))
                .thenReturn(Optional.of(token));

        when(token.getRevokedAt())
                .thenReturn(null);

        when(token.getExpiresAt())
                .thenReturn(OffsetDateTime.now().minusMinutes(1));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.rotate("expired-token")
        );

        verify(repository, never())
                .save(any());
    }

    @Test
    void shouldRevokeActiveRefreshToken() {

        User user = new User(
                "user@example.com",
                "hash",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        setUserId(user, 1L);

        RefreshToken token = mock(RefreshToken.class);

        when(hasher.hash("raw-token"))
                .thenReturn("token-hash");

        when(repository.findByTokenHash("token-hash"))
                .thenReturn(Optional.of(token));

        when(token.getUser())
                .thenReturn(user);

        when(token.getRevokedAt())
                .thenReturn(null);

        service.revoke("raw-token", 1L);

        verify(token)
                .setRevokedAt(any(OffsetDateTime.class));

        verify(repository)
                .save(token);
    }

    @Test
    void shouldNotSaveAlreadyRevokedRefreshToken() {

        User user = new User(
                "user@example.com",
                "hash",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        setUserId(user, 1L);

        RefreshToken token = mock(RefreshToken.class);

        when(hasher.hash("raw-token"))
                .thenReturn("token-hash");

        when(repository.findByTokenHash("token-hash"))
                .thenReturn(Optional.of(token));

        when(token.getUser())
                .thenReturn(user);

        when(token.getRevokedAt())
                .thenReturn(OffsetDateTime.now());

        service.revoke("raw-token", 1L);

        verify(repository, never())
                .save(any());

        verify(token, never())
                .setRevokedAt(any(OffsetDateTime.class));
    }

    @Test
    void revoke_whenTokenBelongsToAnotherUser_throwsInvalidRefreshToken() {

        User tokenOwner = new User(
                "owner@example.com",
                "hash",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        User authenticatedUser = new User(
                "other@example.com",
                "hash",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        setUserId(tokenOwner, 1L);
        setUserId(authenticatedUser, 2L);

        RefreshToken refreshToken = new RefreshToken(
                tokenOwner,
                "hashed-token",
                OffsetDateTime.now().plusDays(30),
                OffsetDateTime.now()
        );

        when(hasher.hash("raw-token"))
                .thenReturn("hashed-token");

        when(repository.findByTokenHash("hashed-token"))
                .thenReturn(Optional.of(refreshToken));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.revoke("raw-token", 2L)
        );

        verify(repository, never())
                .save(any(RefreshToken.class));
    }
}