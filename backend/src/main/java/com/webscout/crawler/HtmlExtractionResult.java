package com.webscout.crawler;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record HtmlExtractionResult(
        String title,
        NormalizedUrl canonicalUrl,
        String description,
        Instant publishedAt,
        String normalizedText,
        List<NormalizedUrl> discoveredLinks
) {

    public HtmlExtractionResult {
        title = normalizeNullableText(title);
        description = normalizeNullableText(description);

        normalizedText = Objects.requireNonNull(
                normalizedText,
                "Normalized text must not be null"
        );

        discoveredLinks = List.copyOf(
                Objects.requireNonNull(
                        discoveredLinks,
                        "Discovered links must not be null"
                )
        );
    }

    private static String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}