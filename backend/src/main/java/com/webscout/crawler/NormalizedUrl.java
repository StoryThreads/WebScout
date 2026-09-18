package com.webscout.crawler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;

public final class NormalizedUrl {

    private final URI uri;
    private final String value;

    private NormalizedUrl(URI uri) {
        this.uri = uri;
        this.value = uri.toString();
    }

    public static NormalizedUrl parse(String rawUrl) {
        Objects.requireNonNull(rawUrl, "URL must not be null");

        final URI uri;

        try {
            uri = new URI(rawUrl);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "Invalid URL",
                    exception
            );
        }

        return fromUri(uri);
    }

    public static NormalizedUrl resolve(
            NormalizedUrl baseUrl,
            String relativeUrl
    ) {
        Objects.requireNonNull(baseUrl, "Base URL must not be null");
        Objects.requireNonNull(
                relativeUrl,
                "Relative URL must not be null"
        );

        final URI resolved;

        try {
            resolved = baseUrl.uri.resolve(relativeUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid relative URL",
                    exception
            );
        }

        return fromUri(resolved);
    }

    private static NormalizedUrl fromUri(URI input) {

        String scheme = input.getScheme();

        if (scheme == null) {
            throw new IllegalArgumentException(
                    "URL must contain a scheme"
            );
        }

        String normalizedScheme =
                scheme.toLowerCase(Locale.ROOT);

        if (!normalizedScheme.equals("http") &&
                !normalizedScheme.equals("https")) {

            throw new IllegalArgumentException(
                    "URL must use HTTP or HTTPS"
            );
        }

        String host = input.getHost();

        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException(
                    "URL must contain a valid host"
            );
        }

        String normalizedHost =
                host.toLowerCase(Locale.ROOT);

        int port = input.getPort();

        if ((normalizedScheme.equals("http") && port == 80) ||
                (normalizedScheme.equals("https") && port == 443)) {

            port = -1;
        }

        String path = input.getPath();

        if (path == null || path.isEmpty()) {
            path = "/";
        }

        try {
            URI normalized = new URI(
                    normalizedScheme,
                    input.getUserInfo(),
                    normalizedHost,
                    port,
                    path,
                    input.getQuery(),
                    null
            );

            return new NormalizedUrl(normalized);

        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "Unable to normalize URL",
                    exception
            );
        }
    }

    public URI uri() {
        return uri;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (!(other instanceof NormalizedUrl that)) {
            return false;
        }

        return uri.equals(that.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}