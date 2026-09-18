package com.webscout.crawler;

import java.util.Locale;
import java.util.Objects;

public final class CrawlPolicy {

    private final String allowedHost;
    private final String allowedPathPrefix;
    private final int maxPages;

    public CrawlPolicy(
            String allowedHost,
            String allowedPathPrefix,
            int maxPages
    ) {
        this.allowedHost = normalizeHost(allowedHost);
        this.allowedPathPrefix = normalizePathPrefix(allowedPathPrefix);
        this.maxPages = validateMaxPages(maxPages);
    }

    public boolean isAllowed(NormalizedUrl url) {
        Objects.requireNonNull(url, "URL must not be null");

        String host = url.uri().getHost();

        if (host == null ||
                !host.equalsIgnoreCase(allowedHost)) {
            return false;
        }

        String path = url.uri().getPath();

        if (path == null || path.isEmpty()) {
            path = "/";
        }

        return isPathAllowed(path);
    }

    public boolean isHostAllowed(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }

        return normalizeHost(host).equals(allowedHost);
    }

    public boolean isPathAllowed(String path) {
        if (path == null || path.isBlank()) {
            path = "/";
        }

        if (allowedPathPrefix.equals("/")) {
            return true;
        }

        return path.equals(allowedPathPrefix)
                || path.startsWith(allowedPathPrefix + "/");
    }

    public String allowedHost() {
        return allowedHost;
    }

    public String allowedPathPrefix() {
        return allowedPathPrefix;
    }

    public int maxPages() {
        return maxPages;
    }

    private static String normalizeHost(String host) {
        Objects.requireNonNull(host, "Allowed host must not be null");

        String normalized = host.trim()
                .toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "Allowed host must not be blank"
            );
        }

        return normalized;
    }

    private static String normalizePathPrefix(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }

        String normalized = path.trim();

        if (!normalized.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Allowed path prefix must start with '/'"
            );
        }

        if (normalized.contains("#")) {
            throw new IllegalArgumentException(
                    "Allowed path prefix must not contain a fragment"
            );
        }

        if (normalized.length() > 1 &&
                normalized.endsWith("/")) {
            normalized = normalized.substring(
                    0,
                    normalized.length() - 1
            );
        }

        return normalized;
    }

    private static int validateMaxPages(int maxPages) {
        if (maxPages <= 0) {
            throw new IllegalArgumentException(
                    "Maximum pages must be greater than zero"
            );
        }

        return maxPages;
    }
}