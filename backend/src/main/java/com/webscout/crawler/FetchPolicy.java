package com.webscout.crawler;

import java.time.Duration;
import java.util.Objects;

public final class FetchPolicy {

    private final Duration requestTimeout;
    private final long maxResponseBytes;
    private final int maxRedirects;
    private final String userAgent;

    public FetchPolicy(
            Duration requestTimeout,
            long maxResponseBytes,
            int maxRedirects,
            String userAgent
    ) {
        this.requestTimeout = validateTimeout(requestTimeout);
        this.maxResponseBytes = validatePositive(
                maxResponseBytes,
                "Maximum response bytes must be greater than zero"
        );
        this.maxRedirects = validateNonNegative(
                maxRedirects,
                "Maximum redirects must not be negative"
        );
        this.userAgent = validateUserAgent(userAgent);
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    public long maxResponseBytes() {
        return maxResponseBytes;
    }

    public int maxRedirects() {
        return maxRedirects;
    }

    public String userAgent() {
        return userAgent;
    }

    private static Duration validateTimeout(Duration timeout) {
        Objects.requireNonNull(timeout, "Request timeout must not be null");

        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "Request timeout must be greater than zero"
            );
        }

        return timeout;
    }

    private static long validatePositive(long value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static int validateNonNegative(int value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static String validateUserAgent(String userAgent) {
        Objects.requireNonNull(userAgent, "User-Agent must not be null");

        if (userAgent.isBlank()) {
            throw new IllegalArgumentException(
                    "User-Agent must not be blank"
            );
        }

        return userAgent;
    }
}