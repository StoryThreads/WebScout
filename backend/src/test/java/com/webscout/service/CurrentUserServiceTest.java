package com.webscout.service;

import com.webscout.dto.UserResponse;
import com.webscout.entity.User;
import com.webscout.exception.InvalidCredentialsException;
import com.webscout.mapper.UserMapper;
import com.webscout.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentUserServiceTest {

    private UserRepository userRepository;
    private UserMapper userMapper;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userMapper = mock(UserMapper.class);

        userService = new UserServiceImpl(
                userRepository,
                userMapper,
                mock(org.springframework.security.crypto.password.PasswordEncoder.class),
                mock(com.webscout.security.JwtService.class),
                mock(RefreshTokenService.class)
        );
    }

    @Test
    void shouldReturnCurrentUser() {

        User user = new User(
                "user@example.com",
                "hashed-password",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        UserResponse expected = new UserResponse();
        expected.setId(42L);
        expected.setEmail("user@example.com");
        expected.setStatus("ACTIVE");

        when(userRepository.findById(42L))
                .thenReturn(Optional.of(user));

        when(userMapper.toResponse(user))
                .thenReturn(expected);

        UserResponse result =
                userService.getCurrentUser(42L);

        assertEquals(42L, result.getId());
        assertEquals("user@example.com", result.getEmail());
        assertEquals("ACTIVE", result.getStatus());

        verify(userRepository).findById(42L);
        verify(userMapper).toResponse(user);
    }

    @Test
    void shouldRejectWhenCurrentUserDoesNotExist() {

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> userService.getCurrentUser(999L)
        );

        verify(userRepository).findById(999L);
        verifyNoInteractions(userMapper);
    }
}