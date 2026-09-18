package com.webscout.crawler;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpFetcherTest {

    @Test
    void shouldExposeFetchResultThroughFetcherContract() {
        NormalizedUrl url =
                NormalizedUrl.parse(
                        "https://example.com/page"
                );

        FetchPolicy policy =
                new FetchPolicy(
                        Duration.ofSeconds(10),
                        1_000_000,
                        5,
                        "WebScoutBot/1.0"
                );

        HttpFetcher fetcher =
                (requestedUrl, fetchPolicy) ->
                        FetchResult.success(
                                requestedUrl,
                                requestedUrl,
                                200,
                                "text/html",
                                "<html>test</html>".getBytes(),
                                25,
                                Map.of(
                                        "content-type",
                                        "text/html"
                                )
                        );

        FetchResult result =
                fetcher.fetch(url, policy);

        assertEquals(url, result.requestedUrl());
        assertEquals(url, result.finalUrl());
        assertEquals(200, result.statusCode());
        assertEquals("text/html", result.contentType());
        assertEquals(25, result.latencyMillis());
        assertArrayEquals(
                "<html>test</html>".getBytes(),
                result.body()
        );
    }

    @Test
    void shouldKeepResponseHeadersImmutable() {
        FetchResult result =
                FetchResult.success(
                        NormalizedUrl.parse(
                                "https://example.com"
                        ),
                        NormalizedUrl.parse(
                                "https://example.com"
                        ),
                        200,
                        "text/html",
                        new byte[0],
                        10,
                        Map.of("server", "test")
                );

        assertEquals(
                "test",
                result.headers().get("server")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.headers().put(
                        "x-test",
                        "value"
                )
        );
    }

    @Test
    void shouldDefensivelyCopyResponseBody() {
        byte[] body = "test".getBytes();

        FetchResult result =
                FetchResult.success(
                        NormalizedUrl.parse(
                                "https://example.com"
                        ),
                        NormalizedUrl.parse(
                                "https://example.com"
                        ),
                        200,
                        "text/html",
                        body,
                        10,
                        Map.of()
                );

        body[0] = 'X';

        assertArrayEquals(
                "test".getBytes(),
                result.body()
        );
    }

    @Test
    void shouldRejectInvalidStatusCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FetchResult.success(
                        NormalizedUrl.parse(
                                "https://example.com"
                        ),
                        NormalizedUrl.parse(
                                "https://example.com"
                        ),
                        99,
                        "text/html",
                        new byte[0],
                        10,
                        Map.of()
                )
        );
    }

    @Test
    void shouldRejectNegativeLatency() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FetchResult.success(
                        NormalizedUrl.parse(
                                "https://example.com"
                        ),
                        NormalizedUrl.parse(
                                "https://example.com"
                        ),
                        200,
                        "text/html",
                        new byte[0],
                        -1,
                        Map.of()
                )
        );
    }
}