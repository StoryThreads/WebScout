package com.webscout.service;

import com.webscout.dto.*;

public interface UserService {

    UserResponse register(RegisterUserRequest request);

    LoginResponse login(LoginRequest request);

    RefreshTokenResponse refresh(RefreshTokenRequest request);

    UserResponse getCurrentUser(Long userId);

    void logout(LogoutRequest request, Long userId);
}