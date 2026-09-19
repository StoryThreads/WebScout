package com.webscout.crawler;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsoupHtmlExtractorTest {

    private static final NormalizedUrl BASE_URL =
            NormalizedUrl.parse(
                    "https://example.com/articles/start"
            );

    private final HtmlExtractor extractor =
            new JsoupHtmlExtractor();

    @Test
    void shouldExtractArticleMetadataTextAndLinks() {

        String html = """
                <html>
                  <head>
                    <title>  WebScout Article  </title>

                    <meta
                        name="description"
                        content="Research description">

                    <link
                        rel="canonical"
                        href="/articles/canonical#fragment">

                    <meta
                        property="article:published_time"
                        content="2026-09-18T10:15:30Z">
                  </head>

                  <body>

                    <nav>
                      Ignore navigation
                    </nav>

                    <main>
                      <h1>Web Intelligence</h1>

                      <p>First   paragraph.</p>

                      <p>Second paragraph.</p>

                      <script>
                        ignore();
                      </script>

                      <style>
                        .hidden { display: none; }
                      </style>

                      <a href="/articles/next">
                        Next
                      </a>

                      <a href="https://example.com/articles/next#fragment">
                        Duplicate
                      </a>
                    </main>

                  </body>
                </html>
                """;

        HtmlExtractionResult result =
                extractor.extract(
                        BASE_URL,
                        html
                );

        assertEquals(
                "WebScout Article",
                result.title()
        );

        assertEquals(
                NormalizedUrl.parse(
                        "https://example.com/articles/canonical"
                ),
                result.canonicalUrl()
        );

        assertEquals(
                "Research description",
                result.description()
        );

        assertEquals(
                Instant.parse(
                        "2026-09-18T10:15:30Z"
                ),
                result.publishedAt()
        );

        assertEquals(
                "Web Intelligence First paragraph. Second paragraph. Next Duplicate",
                result.normalizedText()
        );

        assertEquals(
                List.of(
                        NormalizedUrl.parse(
                                "https://example.com/articles/next"
                        )
                ),
                result.discoveredLinks()
        );
    }

    @Test
    void shouldResolveRelativeLinksAndRemoveFragments() {

        String html = """
                <html>
                  <body>

                    <a href="../docs/page">
                        Relative
                    </a>

                    <a href="#section">
                        Fragment only
                    </a>

                    <a href="mailto:test@example.com">
                        Mail
                    </a>

                    <a href="javascript:void(0)">
                        Script
                    </a>

                  </body>
                </html>
                """;

        HtmlExtractionResult result =
                extractor.extract(
                        BASE_URL,
                        html
                );

        assertEquals(
                List.of(
                        NormalizedUrl.parse(
                                "https://example.com/docs/page"
                        ),
                        NormalizedUrl.parse(
                                "https://example.com/articles/start"
                        )
                ),
                result.discoveredLinks()
        );
    }

    @Test
    void shouldHandlePageWithoutCanonicalOrPublicationDate() {

        String html = """
                <html>
                  <head>
                    <title>Simple page</title>
                  </head>

                  <body>
                    <p>Hello</p>
                  </body>
                </html>
                """;

        HtmlExtractionResult result =
                extractor.extract(
                        BASE_URL,
                        html
                );

        assertEquals(
                "Simple page",
                result.title()
        );

        assertNull(
                result.canonicalUrl()
        );

        assertNull(
                result.description()
        );

        assertNull(
                result.publishedAt()
        );

        assertEquals(
                "Hello",
                result.normalizedText()
        );

        assertTrue(
                result.discoveredLinks().isEmpty()
        );
    }

    @Test
    void shouldFallbackToOpenGraphDescription() {

        String html = """
                <html>
                  <head>
                    <meta
                        property="og:description"
                        content="Open Graph description">
                  </head>

                  <body></body>
                </html>
                """;

        HtmlExtractionResult result =
                extractor.extract(
                        BASE_URL,
                        html
                );

        assertEquals(
                "Open Graph description",
                result.description()
        );
    }

    @Test
    void shouldExtractPublicationDateFromTimeElement() {

        String html = """
                <html>
                  <body>

                    <time datetime="2026-09-19T08:00:00+05:30">
                        19 September 2026
                    </time>

                  </body>
                </html>
                """;

        HtmlExtractionResult result =
                extractor.extract(
                        BASE_URL,
                        html
                );

        assertEquals(
                Instant.parse(
                        "2026-09-19T02:30:00Z"
                ),
                result.publishedAt()
        );
    }

    @Test
    void shouldHandleMalformedHtml() {

        String html = """
                <html>
                  <head>
                    <title>Broken</title>
                  </head>

                  <body>
                    <p>Hello
                    <strong>WebScout
                    <p>World
                  </body>
                </html>
                """;

        HtmlExtractionResult result =
                extractor.extract(
                        BASE_URL,
                        html
                );

        assertEquals(
                "Broken",
                result.title()
        );

        assertTrue(
                result.normalizedText()
                        .contains(
                                "Hello WebScout World"
                        )
        );
    }

    @Test
    void shouldHandlePageWithoutTitle() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head></head>
            <body>
                <main>
                    <p>Content without a title.</p>
                </main>
            </body>
            </html>
            """;

        HtmlExtractionResult result = extractor.extract(
                NormalizedUrl.parse("https://example.com/article"),
                html
        );

        assertNull(result.title());

        assertEquals(
                "Content without a title.",
                result.normalizedText()
        );
    }
}