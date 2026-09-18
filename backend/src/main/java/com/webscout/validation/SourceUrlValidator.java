package com.webscout.validation;

import java.net.URI;
import java.net.URISyntaxException;

public final class SourceUrlValidator {

    private SourceUrlValidator() {
    }

    public static void validateBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Source URL must not be blank");
        }

        final URI uri;

        try {
            uri = new URI(baseUrl);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid source URL");
        }

        String scheme = uri.getScheme();

        if (scheme == null ||
                (!scheme.equalsIgnoreCase("http") &&
                        !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException(
                    "Source URL must use HTTP or HTTPS"
            );
        }

        if (uri.getHost() == null ||
                uri.getHost().isBlank()) {
            throw new IllegalArgumentException(
                    "Source URL must contain a valid host"
            );
        }

        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException(
                    "Source URL must not contain user information"
            );
        }

        if (uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "Source URL must not contain a fragment"
            );
        }
    }

    public static void validateAllowedPathPrefix(
            String allowedPathPrefix
    ) {

        if (allowedPathPrefix == null || allowedPathPrefix.isBlank()) {
            return;
        }

        if (!allowedPathPrefix.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Allowed path prefix must start with '/'"
            );
        }

        if (allowedPathPrefix.contains("#")) {
            throw new IllegalArgumentException(
                    "Allowed path prefix must not contain a fragment"
            );
        }
    }
}