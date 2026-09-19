package com.webscout.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public final class JsoupHtmlExtractor implements HtmlExtractor {

    private static final String NON_CONTENT_SELECTORS =
            "script, style, noscript, template, nav";

    private static final String PUBLICATION_DATE_SELECTORS =
            "meta[property='article:published_time'], "
                    + "meta[property='og:published_time'], "
                    + "meta[name='datePublished'], "
                    + "meta[name='datepublished'], "
                    + "meta[name='date'], "
                    + "time[datetime]";

    @Override
    public HtmlExtractionResult extract(
            NormalizedUrl baseUrl,
            String html
    ) {
        Objects.requireNonNull(
                baseUrl,
                "Base URL must not be null"
        );

        Objects.requireNonNull(
                html,
                "HTML must not be null"
        );

        Document document = Jsoup.parse(
                html,
                baseUrl.value()
        );

        String title = normalizeText(
                document.title()
        );

        String description =
                extractDescription(document);

        NormalizedUrl canonicalUrl =
                extractCanonicalUrl(document);

        Instant publishedAt =
                extractPublishedAt(document);

        String normalizedText;

        Set<NormalizedUrl> discoveredLinks =
                new LinkedHashSet<>();

        Element body = document.body();

        if (body == null) {
            normalizedText = "";
        } else {

            body.select(
                    NON_CONTENT_SELECTORS
            ).remove();

            normalizedText =
                    normalizeText(body.text());
        }

        for (Element link :
                document.select("a[href]")) {

            String absoluteUrl =
                    link.absUrl("href");

            if (absoluteUrl.isBlank()) {
                continue;
            }

            try {

                discoveredLinks.add(
                        NormalizedUrl.parse(
                                absoluteUrl
                        )
                );

            } catch (IllegalArgumentException ignored) {
                /*
                 * Ignore malformed or unsupported
                 * link targets.
                 *
                 * Crawl policy and scope enforcement
                 * belong to the crawler orchestration
                 * layer, not HTML extraction.
                 */
            }
        }

        return new HtmlExtractionResult(
                title,
                canonicalUrl,
                description,
                publishedAt,
                normalizedText,
                List.copyOf(discoveredLinks)
        );
    }

    private NormalizedUrl extractCanonicalUrl(
            Document document
    ) {
        Element canonical =
                document.selectFirst(
                        "link[rel=canonical][href]"
                );

        if (canonical == null) {
            return null;
        }

        String absoluteUrl =
                canonical.absUrl("href");

        if (absoluteUrl.isBlank()) {
            return null;
        }

        try {

            return NormalizedUrl.parse(
                    absoluteUrl
            );

        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String extractDescription(
            Document document
    ) {
        Element description =
                document.selectFirst(
                        "meta[name=description][content]"
                );

        if (description == null) {
            description =
                    document.selectFirst(
                            "meta[property='og:description'][content]"
                    );
        }

        if (description == null) {
            return null;
        }

        return normalizeText(
                description.attr("content")
        );
    }

    private Instant extractPublishedAt(
            Document document
    ) {
        for (Element element :
                document.select(
                        PUBLICATION_DATE_SELECTORS
                )) {

            String candidate =
                    element.hasAttr("datetime")
                            ? element.attr("datetime")
                            : element.attr("content");

            Instant parsed =
                    parseInstant(candidate);

            if (parsed != null) {
                return parsed;
            }
        }

        return null;
    }

    private Instant parseInstant(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String candidate =
                value.trim();

        try {
            return Instant.parse(candidate);
        } catch (java.time.format.DateTimeParseException ignored) {
            // Try ISO-8601 with an explicit offset.
        }

        try {
            return OffsetDateTime
                    .parse(candidate)
                    .toInstant();

        } catch (java.time.format.DateTimeParseException ignored) {
            return null;
        }
    }

    private String normalizeText(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return value == null
                    ? null
                    : "";
        }

        return value
                .replaceAll("\\s+", " ")
                .trim();
    }
}