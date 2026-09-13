package com.webscout.service;

import com.webscout.dto.LoginRequest;
import com.webscout.dto.LoginResponse;
import com.webscout.dto.RegisterUserRequest;
import com.webscout.dto.UserResponse;

public interface UserService {

    UserResponse register(RegisterUserRequest request);
    LoginResponse login(LoginRequest request);

}