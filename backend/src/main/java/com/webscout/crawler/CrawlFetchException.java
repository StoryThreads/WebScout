package com.webscout.crawler;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class CrawlFetchException extends RuntimeException {

    private final CrawlErrorType errorType;
    private final int statusCode;
    private final Map<String, String> headers;

    public CrawlFetchException(
            CrawlErrorType errorType,
            String message,
            Throwable cause
    ) {
        this(
                errorType,
                message,
                cause,
                -1,
                Map.of()
        );
    }

    public CrawlFetchException(
            CrawlErrorType errorType,
            String message,
            Throwable cause,
            int statusCode,
            Map<String, String> headers
    ) {
        super(message, cause);

        this.errorType = Objects.requireNonNull(
                errorType,
                "Error type must not be null"
        );

        if (statusCode < -1 || statusCode > 599) {
            throw new IllegalArgumentException(
                    "Invalid HTTP status code"
            );
        }

        this.statusCode = statusCode;

        this.headers =
                headers == null
                        ? Map.of()
                        : Collections.unmodifiableMap(
                        Map.copyOf(headers)
                );
    }

    public CrawlErrorType errorType() {
        return errorType;
    }

    public int statusCode() {
        return statusCode;
    }

    public Map<String, String> headers() {
        return headers;
    }
}