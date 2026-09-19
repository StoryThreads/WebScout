package com.webscout.crawler;

import java.util.Objects;

public final class CrawlFetchException extends RuntimeException {

    private final CrawlErrorType errorType;

    public CrawlFetchException(
            CrawlErrorType errorType,
            String message,
            Throwable cause
    ) {
        super(message, cause);

        this.errorType = Objects.requireNonNull(
                errorType,
                "Error type must not be null"
        );
    }

    public CrawlErrorType errorType() {
        return errorType;
    }
}