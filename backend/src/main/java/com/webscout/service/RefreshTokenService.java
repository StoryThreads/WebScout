package com.webscout.service;

import com.webscout.entity.RefreshToken;
import com.webscout.entity.User;

public interface RefreshTokenService {
    RefreshTokenResult create(User user);
}
