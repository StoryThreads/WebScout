package com.webscout.security;

import com.webscout.entity.User;

public interface JwtService {

    String generateAccessToken(User user);

    Long extractUserId(String token);

}