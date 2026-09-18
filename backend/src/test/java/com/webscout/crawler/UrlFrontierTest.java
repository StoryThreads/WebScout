package com.webscout.crawler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlFrontierTest {

    @Test
    void shouldAddAndPollUrl() {
        UrlFrontier frontier = new UrlFrontier();

        NormalizedUrl url =
                NormalizedUrl.parse("https://example.com/page");

        assertTrue(frontier.add(url));
        assertEquals(1, frontier.size());

        assertEquals(url, frontier.poll());
        assertTrue(frontier.isEmpty());
    }

    @Test
    void shouldRejectDuplicateUrl() {
        UrlFrontier frontier = new UrlFrontier();

        NormalizedUrl url =
                NormalizedUrl.parse("https://example.com/page");

        assertTrue(frontier.add(url));
        assertFalse(frontier.add(url));

        assertEquals(1, frontier.size());
    }

    @Test
    void equivalentNormalizedUrlsShouldBeDeduplicated() {
        UrlFrontier frontier = new UrlFrontier();

        NormalizedUrl first =
                NormalizedUrl.parse("HTTPS://EXAMPLE.COM:443/page#section");

        NormalizedUrl second =
                NormalizedUrl.parse("https://example.com/page");

        assertTrue(frontier.add(first));
        assertFalse(frontier.add(second));

        assertEquals(1, frontier.size());
    }

    @Test
    void shouldMaintainFifoOrder() {
        UrlFrontier frontier = new UrlFrontier();

        NormalizedUrl first =
                NormalizedUrl.parse("https://example.com/first");

        NormalizedUrl second =
                NormalizedUrl.parse("https://example.com/second");

        frontier.add(first);
        frontier.add(second);

        assertEquals(first, frontier.poll());
        assertEquals(second, frontier.poll());
    }

    @Test
    void shouldReportContainsForScheduledUrl() {
        UrlFrontier frontier = new UrlFrontier();

        NormalizedUrl url =
                NormalizedUrl.parse("https://example.com/page");

        assertFalse(frontier.contains(url));

        frontier.add(url);

        assertTrue(frontier.contains(url));
    }

    @Test
    void shouldAllowNullOnlyByRejectingIt() {
        UrlFrontier frontier = new UrlFrontier();

        assertThrows(
                NullPointerException.class,
                () -> frontier.add(null)
        );

        assertThrows(
                NullPointerException.class,
                () -> frontier.contains(null)
        );
    }

    @Test
    void shouldClearFrontier() {
        UrlFrontier frontier = new UrlFrontier();

        frontier.add(
                NormalizedUrl.parse("https://example.com/page")
        );

        frontier.clear();

        assertTrue(frontier.isEmpty());
        assertEquals(0, frontier.size());
    }
}