package com.webscout.service;

import com.webscout.entity.User;

public interface RefreshTokenService {

    RefreshTokenResult create(User user);

    RefreshTokenResult rotate(String rawToken);

    void revoke(String rawToken, Long userId);
}