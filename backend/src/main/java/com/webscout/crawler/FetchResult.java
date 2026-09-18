package com.webscout.crawler;

import java.util.Map;
import java.util.Objects;

public final class FetchResult {

    private final NormalizedUrl requestedUrl;
    private final NormalizedUrl finalUrl;
    private final int statusCode;
    private final String contentType;
    private final byte[] body;
    private final long latencyMillis;
    private final Map<String, String> headers;

    private FetchResult(
            NormalizedUrl requestedUrl,
            NormalizedUrl finalUrl,
            int statusCode,
            String contentType,
            byte[] body,
            long latencyMillis,
            Map<String, String> headers
    ) {
        this.requestedUrl = Objects.requireNonNull(
                requestedUrl,
                "Requested URL must not be null"
        );

        this.finalUrl = Objects.requireNonNull(
                finalUrl,
                "Final URL must not be null"
        );

        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException(
                    "Invalid HTTP status code"
            );
        }

        if (contentType != null && contentType.isBlank()) {
            throw new IllegalArgumentException(
                    "Content type must not be blank"
            );
        }

        this.statusCode = statusCode;
        this.contentType = contentType;
        this.body = body == null ? new byte[0] : body.clone();

        if (latencyMillis < 0) {
            throw new IllegalArgumentException(
                    "Latency must not be negative"
            );
        }

        this.latencyMillis = latencyMillis;
        this.headers = headers == null
                ? Map.of()
                : Map.copyOf(headers);
    }

    public static FetchResult success(
            NormalizedUrl requestedUrl,
            NormalizedUrl finalUrl,
            int statusCode,
            String contentType,
            byte[] body,
            long latencyMillis,
            Map<String, String> headers
    ) {
        return new FetchResult(
                requestedUrl,
                finalUrl,
                statusCode,
                contentType,
                body,
                latencyMillis,
                headers
        );
    }

    public NormalizedUrl requestedUrl() {
        return requestedUrl;
    }

    public NormalizedUrl finalUrl() {
        return finalUrl;
    }

    public int statusCode() {
        return statusCode;
    }

    public String contentType() {
        return contentType;
    }

    public byte[] body() {
        return body.clone();
    }

    public long latencyMillis() {
        return latencyMillis;
    }

    public Map<String, String> headers() {
        return headers;
    }
}