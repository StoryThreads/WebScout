package com.webscout.dto;

import jakarta.validation.constraints.NotBlank;

public class LogoutRequest {

    @NotBlank
    private String refreshToken;

    public LogoutRequest() {
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}