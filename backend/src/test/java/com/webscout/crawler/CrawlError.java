package com.webscout.crawler;

import java.util.Objects;

public final class CrawlError {

    private final CrawlErrorType type;
    private final String message;

    public CrawlError(CrawlErrorType type, String message) {
        this.type = Objects.requireNonNull(
                type,
                "Error type must not be null"
        );

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Error message must not be blank"
            );
        }

        this.message = message;
    }

    public CrawlErrorType type() {
        return type;
    }

    public String message() {
        return message;
    }
}