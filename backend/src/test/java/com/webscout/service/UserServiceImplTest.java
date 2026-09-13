package com.webscout.service;

import com.webscout.dto.LoginRequest;
import com.webscout.dto.LoginResponse;
import com.webscout.dto.RegisterUserRequest;
import com.webscout.dto.UserResponse;
import com.webscout.entity.User;
import com.webscout.exception.EmailAlreadyExistsException;
import com.webscout.exception.InvalidCredentialsException;
import com.webscout.mapper.UserMapper;
import com.webscout.repository.UserRepository;
import com.webscout.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    private UserRepository userRepository;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private RefreshTokenService refreshTokenService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {

        userRepository = mock(UserRepository.class);
        userMapper = mock(UserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        refreshTokenService = mock(RefreshTokenService.class);

        userService = new UserServiceImpl(
                userRepository,
                userMapper,
                passwordEncoder,
                jwtService,
                refreshTokenService
        );
    }

    @Test
    void shouldRegisterUserSuccessfully() {

        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("  TEST@Example.COM ");
        request.setPassword("password123");

        User savedUser = new User(
                "test@example.com",
                "hashed-password",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(1L);
        expectedResponse.setEmail("test@example.com");
        expectedResponse.setStatus("ACTIVE");

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("hashed-password");

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        when(userMapper.toResponse(savedUser))
                .thenReturn(expectedResponse);

        UserResponse result = userService.register(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("ACTIVE", result.getStatus());

        verify(userRepository)
                .existsByEmail("test@example.com");

        verify(passwordEncoder)
                .encode("password123");

        verify(userRepository)
                .save(any(User.class));

        verify(userMapper)
                .toResponse(savedUser);
    }

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {

        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("existing@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.register(request)
        );

        verify(userRepository)
                .existsByEmail("existing@example.com");

        verify(passwordEncoder, never())
                .encode(any());

        verify(userRepository, never())
                .save(any());

        verifyNoInteractions(userMapper);
    }

    @Test
    void shouldLoginSuccessfully() {

        LoginRequest request = new LoginRequest();
        request.setEmail("  TEST@Example.COM ");
        request.setPassword("password123");

        User user = new User(
                "test@example.com",
                "hashed-password",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        RefreshTokenResult refreshTokenResult =
                mock(RefreshTokenResult.class);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                "hashed-password"
        )).thenReturn(true);

        when(jwtService.generateAccessToken(user))
                .thenReturn("access-token");

        when(refreshTokenService.create(user))
                .thenReturn(refreshTokenResult);

        when(refreshTokenResult.rawToken())
                .thenReturn("refresh-token");

        LoginResponse result = userService.login(request);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());

        verify(userRepository)
                .findByEmail("test@example.com");

        verify(passwordEncoder)
                .matches("password123", "hashed-password");

        verify(jwtService)
                .generateAccessToken(user);

        verify(refreshTokenService)
                .create(user);
    }

    @Test
    void shouldRejectLoginWhenEmailDoesNotExist() {

        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> userService.login(request)
        );

        verify(userRepository)
                .findByEmail("unknown@example.com");

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void shouldRejectLoginWhenPasswordIsIncorrect() {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong-password");

        User user = new User(
                "test@example.com",
                "hashed-password",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "hashed-password"
        )).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> userService.login(request)
        );

        verify(passwordEncoder)
                .matches("wrong-password", "hashed-password");

        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void shouldRejectLoginWhenUserIsInactive() {

        LoginRequest request = new LoginRequest();
        request.setEmail("inactive@example.com");
        request.setPassword("password123");

        User user = new User(
                "inactive@example.com",
                "hashed-password",
                "DISABLED",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(userRepository.findByEmail("inactive@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                "hashed-password"
        )).thenReturn(true);

        assertThrows(
                InvalidCredentialsException.class,
                () -> userService.login(request)
        );

        verify(passwordEncoder)
                .matches("password123", "hashed-password");

        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
    }
}