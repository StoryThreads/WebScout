package com.webscout.crawler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrawlPolicyTest {

    @Test
    void shouldAllowSameHostAndPath() {
        CrawlPolicy policy = new CrawlPolicy(
                "example.com",
                "/docs",
                100
        );

        NormalizedUrl url =
                NormalizedUrl.parse("https://example.com/docs/start");

        assertTrue(policy.isAllowed(url));
    }

    @Test
    void shouldRejectDifferentHost() {
        CrawlPolicy policy = new CrawlPolicy(
                "example.com",
                "/docs",
                100
        );

        NormalizedUrl url =
                NormalizedUrl.parse("https://other.com/docs/start");

        assertFalse(policy.isAllowed(url));
    }

    @Test
    void shouldRejectPathOutsideScope() {
        CrawlPolicy policy = new CrawlPolicy(
                "example.com",
                "/docs",
                100
        );

        NormalizedUrl url =
                NormalizedUrl.parse("https://example.com/blog/start");

        assertFalse(policy.isAllowed(url));
    }

    @Test
    void shouldAllowExactPathPrefix() {
        CrawlPolicy policy = new CrawlPolicy(
                "example.com",
                "/docs",
                100
        );

        NormalizedUrl url =
                NormalizedUrl.parse("https://example.com/docs");

        assertTrue(policy.isAllowed(url));
    }

    @Test
    void shouldAllowNestedPath() {
        CrawlPolicy policy = new CrawlPolicy(
                "example.com",
                "/docs",
                100
        );

        NormalizedUrl url =
                NormalizedUrl.parse("https://example.com/docs/api/v1");

        assertTrue(policy.isAllowed(url));
    }

    @Test
    void shouldRejectLookalikePath() {
        CrawlPolicy policy = new CrawlPolicy(
                "example.com",
                "/docs",
                100
        );

        NormalizedUrl url =
                NormalizedUrl.parse("https://example.com/docs-other");

        assertFalse(policy.isAllowed(url));
    }

    @Test
    void shouldNormalizeHostCase() {
        CrawlPolicy policy = new CrawlPolicy(
                "EXAMPLE.COM",
                "/docs",
                100
        );

        assertEquals("example.com", policy.allowedHost());
    }

    @Test
    void shouldNormalizeTrailingPathSlash() {
        CrawlPolicy policy = new CrawlPolicy(
                "example.com",
                "/docs/",
                100
        );

        assertEquals("/docs", policy.allowedPathPrefix());
    }

    @Test
    void shouldAllowEverythingWhenPathPrefixIsRoot() {
        CrawlPolicy policy = new CrawlPolicy(
                "example.com",
                "/",
                100
        );

        NormalizedUrl url =
                NormalizedUrl.parse("https://example.com/anything/here");

        assertTrue(policy.isAllowed(url));
    }

    @Test
    void shouldRejectZeroMaxPages() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CrawlPolicy(
                        "example.com",
                        "/docs",
                        0
                )
        );
    }

    @Test
    void shouldRejectNegativeMaxPages() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CrawlPolicy(
                        "example.com",
                        "/docs",
                        -1
                )
        );
    }

    @Test
    void shouldRejectInvalidPathPrefix() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CrawlPolicy(
                        "example.com",
                        "docs",
                        100
                )
        );
    }
    @Test
    void shouldRejectRedirectTargetOutsideScope() {

        CrawlPolicy policy =
                new CrawlPolicy(
                        "example.com",
                        "/docs",
                        100
                );

        NormalizedUrl redirectTarget =
                NormalizedUrl.parse(
                        "https://other.com/docs/page"
                );

        assertFalse(
                policy.isAllowed(redirectTarget)
        );
    }
}