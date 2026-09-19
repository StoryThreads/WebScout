package com.webscout.crawler;

public interface HtmlExtractor {

    /**
     * Parses already-fetched HTML into crawler-level structured data.
     *
     * <p>This component does not perform HTTP requests, persistence,
     * robots checks, scope checks, or relevance scoring.</p>
     */
    HtmlExtractionResult extract(
            NormalizedUrl baseUrl,
            String html
    );
}