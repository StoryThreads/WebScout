package com.webscout.crawler;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CrawlContextTest {

    @Test
    void shouldCreateContextWithRequiredPolicies() {
        CrawlPolicy crawlPolicy =
                new CrawlPolicy("example.com", "/", 10);

        FetchPolicy fetchPolicy =
                new FetchPolicy(
                        Duration.ofSeconds(10),
                        1_000_000,
                        5,
                        "WebScoutBot/1.0"
                );

        RobotsPolicy robotsPolicy =
                new TestRobotsPolicy(true);

        UrlFrontier frontier = new UrlFrontier();

        CrawlContext context = new CrawlContext(
                crawlPolicy,
                fetchPolicy,
                robotsPolicy,
                frontier
        );

        assertSame(crawlPolicy, context.crawlPolicy());
        assertSame(fetchPolicy, context.fetchPolicy());
        assertSame(robotsPolicy, context.robotsPolicy());
        assertSame(frontier, context.frontier());
    }

    @Test
    void shouldStartWithZeroProcessedPages() {
        CrawlContext context = createContext(10);

        assertEquals(0, context.pagesProcessed());
        assertTrue(context.canProcessMorePages());
    }

    @Test
    void shouldIncrementProcessedPages() {
        CrawlContext context = createContext(10);

        context.markPageProcessed();
        context.markPageProcessed();

        assertEquals(2, context.pagesProcessed());
    }

    @Test
    void shouldStopWhenMaximumPagesReached() {
        CrawlContext context = createContext(2);

        context.markPageProcessed();
        context.markPageProcessed();

        assertEquals(2, context.pagesProcessed());
        assertFalse(context.canProcessMorePages());
    }

    @Test
    void shouldRejectProcessingBeyondMaximumPages() {
        CrawlContext context = createContext(1);

        context.markPageProcessed();

        assertThrows(
                IllegalStateException.class,
                context::markPageProcessed
        );
    }

    @Test
    void shouldSupportCancellation() {
        CrawlContext context = createContext(10);

        assertFalse(context.isCancelled());
        assertTrue(context.canProcessMorePages());

        context.cancel();

        assertTrue(context.isCancelled());
        assertFalse(context.canProcessMorePages());
    }

    @Test
    void shouldRejectNullDependencies() {
        CrawlPolicy crawlPolicy =
                new CrawlPolicy("example.com", "/", 10);

        FetchPolicy fetchPolicy =
                new FetchPolicy(
                        Duration.ofSeconds(10),
                        1_000_000,
                        5,
                        "WebScoutBot/1.0"
                );

        RobotsPolicy robotsPolicy =
                new TestRobotsPolicy(true);

        UrlFrontier frontier = new UrlFrontier();

        assertThrows(
                NullPointerException.class,
                () -> new CrawlContext(
                        null,
                        fetchPolicy,
                        robotsPolicy,
                        frontier
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new CrawlContext(
                        crawlPolicy,
                        null,
                        robotsPolicy,
                        frontier
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new CrawlContext(
                        crawlPolicy,
                        fetchPolicy,
                        null,
                        frontier
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new CrawlContext(
                        crawlPolicy,
                        fetchPolicy,
                        robotsPolicy,
                        null
                )
        );
    }

    private static CrawlContext createContext(int maxPages) {
        return new CrawlContext(
                new CrawlPolicy(
                        "example.com",
                        "/",
                        maxPages
                ),
                new FetchPolicy(
                        Duration.ofSeconds(10),
                        1_000_000,
                        5,
                        "WebScoutBot/1.0"
                ),
                new TestRobotsPolicy(true),
                new UrlFrontier()
        );
    }

    private static final class TestRobotsPolicy
            implements RobotsPolicy {

        private final boolean allowed;

        private TestRobotsPolicy(boolean allowed) {
            this.allowed = allowed;
        }

        @Override
        public boolean isAllowed(NormalizedUrl url) {
            return allowed;
        }

        @Override
        public Duration crawlDelay() {
            return null;
        }
    }
}