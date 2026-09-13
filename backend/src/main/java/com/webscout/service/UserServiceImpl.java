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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Locale;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public UserResponse register(RegisterUserRequest request) {

        // 1. Normalize email
        String normalizedEmail =
                request.getEmail().trim().toLowerCase(Locale.ROOT);

        // 2. Check duplicate email
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        // 3. Hash password
        String passwordHash =
                passwordEncoder.encode(request.getPassword());

        // 4. Determine initial state
        String status = "ACTIVE";

        // 5. Establish timestamps
        OffsetDateTime now = OffsetDateTime.now();

        // 6. Create user
        User user = new User(
                normalizedEmail,
                passwordHash,
                status,
                now,
                now
        );

        // 7. Persist
        User savedUser = userRepository.save(user);

        // 8. Convert entity → response DTO
        return userMapper.toResponse(savedUser);
    }

    private User authenticate(LoginRequest request) {

        String normalizedEmail =
                request.getEmail().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {
            throw new InvalidCredentialsException();
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = authenticate(request);

        String accessToken = jwtService.generateAccessToken(user);

        RefreshTokenResult refreshTokenResult =
                refreshTokenService.create(user);

        return new LoginResponse(
                accessToken,
                refreshTokenResult.rawToken()
        );
    }
}