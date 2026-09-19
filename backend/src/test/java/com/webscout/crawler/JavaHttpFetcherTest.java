package com.webscout.crawler;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JavaHttpFetcherTest {

    private HttpServer server;
    private JavaHttpFetcher fetcher;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {

        server = HttpServer.create(
                new InetSocketAddress("localhost", 0),
                0
        );

        fetcher = new JavaHttpFetcher();

        server.start();

        baseUrl =
                "http://localhost:"
                        + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {

        if (server != null) {
            server.stop(0);
        }
    }

    private FetchPolicy policy(
            long maxResponseBytes
    ) {
        return new FetchPolicy(
                Duration.ofSeconds(5),
                maxResponseBytes,
                5,
                "WebScoutBot/1.0"
        );
    }

    @Test
    void shouldFetchSuccessfulHtmlResponse() {

        server.createContext(
                "/hello",
                exchange -> {

                    byte[] response =
                            "<html>Hello WebScout</html>"
                                    .getBytes();

                    exchange.getResponseHeaders()
                            .add(
                                    "Content-Type",
                                    "text/html; charset=UTF-8"
                            );

                    exchange.sendResponseHeaders(
                            200,
                            response.length
                    );

                    try (OutputStream output =
                                 exchange.getResponseBody()) {

                        output.write(response);
                    }
                }
        );

        NormalizedUrl url =
                NormalizedUrl.parse(
                        baseUrl + "/hello"
                );

        FetchResult result =
                fetcher.fetch(
                        url,
                        policy(1_000_000)
                );

        assertEquals(
                200,
                result.statusCode()
        );

        assertEquals(
                url,
                result.requestedUrl()
        );

        assertEquals(
                url,
                result.finalUrl()
        );

        assertEquals(
                "text/html; charset=UTF-8",
                result.contentType()
        );

        assertArrayEquals(
                "<html>Hello WebScout</html>"
                        .getBytes(),
                result.body()
        );

        assertTrue(
                result.latencyMillis() >= 0
        );
    }

    @Test
    void shouldSendConfiguredUserAgent() {

        server.createContext(
                "/user-agent",
                exchange -> {

                    String userAgent =
                            exchange.getRequestHeaders()
                                    .getFirst("User-Agent");

                    byte[] response =
                            userAgent.getBytes();

                    exchange.getResponseHeaders()
                            .add(
                                    "Content-Type",
                                    "text/html"
                            );

                    exchange.sendResponseHeaders(
                            200,
                            response.length
                    );

                    try (OutputStream output =
                                 exchange.getResponseBody()) {

                        output.write(response);
                    }
                }
        );

        FetchPolicy customPolicy =
                new FetchPolicy(
                        Duration.ofSeconds(5),
                        1_000_000,
                        5,
                        "WebScoutBot/2.0"
                );

        FetchResult result =
                fetcher.fetch(
                        NormalizedUrl.parse(
                                baseUrl + "/user-agent"
                        ),
                        customPolicy
                );

        assertArrayEquals(
                "WebScoutBot/2.0".getBytes(),
                result.body()
        );
    }

    @Test
    void shouldFollowRedirect() {

        server.createContext(
                "/start",
                exchange -> {

                    exchange.getResponseHeaders()
                            .add(
                                    "Location",
                                    "/final"
                            );

                    exchange.sendResponseHeaders(
                            302,
                            -1
                    );

                    exchange.close();
                }
        );

        server.createContext(
                "/final",
                exchange -> {

                    byte[] response =
                            "<html>final</html>".getBytes();

                    exchange.getResponseHeaders()
                            .add(
                                    "Content-Type",
                                    "text/html"
                            );

                    exchange.sendResponseHeaders(
                            200,
                            response.length
                    );

                    try (OutputStream output =
                                 exchange.getResponseBody()) {

                        output.write(response);
                    }
                }
        );

        NormalizedUrl startUrl =
                NormalizedUrl.parse(
                        baseUrl + "/start"
                );

        FetchResult result =
                fetcher.fetch(
                        startUrl,
                        policy(1_000_000)
                );

        assertEquals(
                200,
                result.statusCode()
        );

        assertEquals(
                startUrl,
                result.requestedUrl()
        );

        assertEquals(
                NormalizedUrl.parse(
                        baseUrl + "/final"
                ),
                result.finalUrl()
        );
    }

    @Test
    void shouldReturnRedirectWhenRedirectsDisabled() {

        server.createContext(
                "/redirect",
                exchange -> {

                    exchange.getResponseHeaders()
                            .add(
                                    "Location",
                                    "/target"
                            );

                    exchange.sendResponseHeaders(
                            302,
                            -1
                    );

                    exchange.close();
                }
        );

        FetchPolicy noRedirectPolicy =
                new FetchPolicy(
                        Duration.ofSeconds(5),
                        1_000_000,
                        0,
                        "WebScoutBot/1.0"
                );

        NormalizedUrl url =
                NormalizedUrl.parse(
                        baseUrl + "/redirect"
                );

        FetchResult result =
                fetcher.fetch(
                        url,
                        noRedirectPolicy
                );

        assertEquals(
                302,
                result.statusCode()
        );

        assertEquals(
                url,
                result.finalUrl()
        );
    }

    @Test
    void shouldRejectTooManyRedirects() {

        server.createContext(
                "/one",
                exchange -> {

                    exchange.getResponseHeaders()
                            .add(
                                    "Location",
                                    "/two"
                            );

                    exchange.sendResponseHeaders(
                            302,
                            -1
                    );

                    exchange.close();
                }
        );

        server.createContext(
                "/two",
                exchange -> {

                    exchange.getResponseHeaders()
                            .add(
                                    "Location",
                                    "/three"
                            );

                    exchange.sendResponseHeaders(
                            302,
                            -1
                    );

                    exchange.close();
                }
        );

        server.createContext(
                "/three",
                exchange -> {

                    exchange.getResponseHeaders()
                            .add(
                                    "Location",
                                    "/four"
                            );

                    exchange.sendResponseHeaders(
                            302,
                            -1
                    );

                    exchange.close();
                }
        );

        FetchPolicy limitedPolicy =
                new FetchPolicy(
                        Duration.ofSeconds(5),
                        1_000_000,
                        2,
                        "WebScoutBot/1.0"
                );

        CrawlFetchException exception =
                assertThrows(
                        CrawlFetchException.class,
                        () -> fetcher.fetch(
                                NormalizedUrl.parse(
                                        baseUrl + "/one"
                                ),
                                limitedPolicy
                        )
                );

        assertEquals(
                CrawlErrorType.TOO_MANY_REDIRECTS,
                exception.errorType()
        );
    }

    @Test
    void shouldRejectResponseExceedingSizeLimit() {

        server.createContext(
                "/large",
                exchange -> {

                    byte[] response =
                            "12345678901234567890"
                                    .getBytes();

                    exchange.getResponseHeaders()
                            .add(
                                    "Content-Type",
                                    "text/html"
                            );

                    exchange.sendResponseHeaders(
                            200,
                            response.length
                    );

                    try (OutputStream output =
                                 exchange.getResponseBody()) {

                        output.write(response);
                    }
                }
        );

        CrawlFetchException exception =
                assertThrows(
                        CrawlFetchException.class,
                        () -> fetcher.fetch(
                                NormalizedUrl.parse(
                                        baseUrl + "/large"
                                ),
                                policy(10)
                        )
                );

        assertEquals(
                CrawlErrorType.RESPONSE_TOO_LARGE,
                exception.errorType()
        );
    }

    @Test
    void shouldAcceptResponseWithinSizeLimit() {

        server.createContext(
                "/small",
                exchange -> {

                    byte[] response =
                            "small response".getBytes();

                    exchange.getResponseHeaders()
                            .add(
                                    "Content-Type",
                                    "text/html"
                            );

                    exchange.sendResponseHeaders(
                            200,
                            response.length
                    );

                    try (OutputStream output =
                                 exchange.getResponseBody()) {

                        output.write(response);
                    }
                }
        );

        FetchResult result =
                fetcher.fetch(
                        NormalizedUrl.parse(
                                baseUrl + "/small"
                        ),
                        policy(1_000)
                );

        assertEquals(
                200,
                result.statusCode()
        );

        assertArrayEquals(
                "small response".getBytes(),
                result.body()
        );
    }

    @Test
    void shouldRejectNonHtmlContentType() {

        server.createContext(
                "/json",
                exchange -> {

                    byte[] response =
                            "{\"status\":\"ok\"}".getBytes();

                    exchange.getResponseHeaders()
                            .add(
                                    "Content-Type",
                                    "application/json"
                            );

                    exchange.sendResponseHeaders(
                            200,
                            response.length
                    );

                    try (OutputStream output =
                                 exchange.getResponseBody()) {

                        output.write(response);
                    }
                }
        );

        CrawlFetchException exception =
                assertThrows(
                        CrawlFetchException.class,
                        () -> fetcher.fetch(
                                NormalizedUrl.parse(
                                        baseUrl + "/json"
                                ),
                                policy(1_000_000)
                        )
                );

        assertEquals(
                CrawlErrorType.UNSUPPORTED_CONTENT_TYPE,
                exception.errorType()
        );
    }

    @Test
    void shouldClassifyNotFoundResponse() {

        server.createContext(
                "/not-found",
                exchange -> {

                    byte[] response =
                            "<html>Not Found</html>"
                                    .getBytes();

                    exchange.getResponseHeaders()
                            .add(
                                    "Content-Type",
                                    "text/html"
                            );

                    exchange.sendResponseHeaders(
                            404,
                            response.length
                    );

                    try (OutputStream output =
                                 exchange.getResponseBody()) {

                        output.write(response);
                    }
                }
        );

        FetchResult result =
                fetcher.fetch(
                        NormalizedUrl.parse(
                                baseUrl + "/not-found"
                        ),
                        policy(1_000_000)
                );

        assertEquals(
                404,
                result.statusCode()
        );

        assertEquals(
                CrawlErrorType.HTTP_CLIENT_ERROR,
                HttpStatusClassifier.classify(
                        result.statusCode()
                )
        );
    }

    @Test
    void shouldClassifyRateLimitedResponseAndPreserveRetryAfter() {

        server.createContext(
                "/rate-limit",
                exchange -> {

                    byte[] response =
                            "<html>Too many requests</html>"
                                    .getBytes();

                    exchange.getResponseHeaders()
                            .add(
                                    "Content-Type",
                                    "text/html"
                            );

                    exchange.getResponseHeaders()
                            .add(
                                    "Retry-After",
                                    "30"
                            );

                    exchange.sendResponseHeaders(
                            429,
                            response.length
                    );

                    try (OutputStream output =
                                 exchange.getResponseBody()) {

                        output.write(response);
                    }
                }
        );

        FetchResult result =
                fetcher.fetch(
                        NormalizedUrl.parse(
                                baseUrl + "/rate-limit"
                        ),
                        policy(1_000_000)
                );

        assertEquals(
                429,
                result.statusCode()
        );

        assertEquals(
                "30",
                result.headers()
                        .entrySet()
                        .stream()
                        .filter(entry ->
                                entry.getKey()
                                        .equalsIgnoreCase("Retry-After")
                        )
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null)
        );

        assertEquals(
                CrawlErrorType.HTTP_RATE_LIMITED,
                HttpStatusClassifier.classify(
                        result.statusCode()
                )
        );
    }

    @Test
    void shouldClassifyServerErrors() {

        server.createContext(
                "/server-error",
                exchange -> {

                    byte[] response =
                            "<html>Server error</html>"
                                    .getBytes();

                    exchange.getResponseHeaders()
                            .add(
                                    "Content-Type",
                                    "text/html"
                            );

                    exchange.sendResponseHeaders(
                            503,
                            response.length
                    );

                    try (OutputStream output =
                                 exchange.getResponseBody()) {

                        output.write(response);
                    }
                }
        );

        FetchResult result =
                fetcher.fetch(
                        NormalizedUrl.parse(
                                baseUrl + "/server-error"
                        ),
                        policy(1_000_000)
                );

        assertEquals(
                503,
                result.statusCode()
        );

        assertEquals(
                CrawlErrorType.HTTP_SERVER_ERROR,
                HttpStatusClassifier.classify(
                        result.statusCode()
                )
        );
    }

    @Test
    void shouldMeasureLatency() {

        server.createContext(
                "/latency",
                exchange -> {

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }

                    byte[] response =
                            "<html>Delayed</html>"
                                    .getBytes();

                    exchange.getResponseHeaders()
                            .add(
                                    "Content-Type",
                                    "text/html"
                            );

                    exchange.sendResponseHeaders(
                            200,
                            response.length
                    );

                    try (OutputStream output =
                                 exchange.getResponseBody()) {

                        output.write(response);
                    }
                }
        );

        FetchResult result =
                fetcher.fetch(
                        NormalizedUrl.parse(
                                baseUrl + "/latency"
                        ),
                        policy(1_000_000)
                );

        assertTrue(
                result.latencyMillis() >= 50
        );
    }

    @Test
    void shouldRejectRequestTimeout() {

        server.createContext(
                "/slow",
                exchange -> {

                    try {
                        Thread.sleep(2_000);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                }
        );

        FetchPolicy timeoutPolicy =
                new FetchPolicy(
                        Duration.ofMillis(100),
                        1_000_000,
                        5,
                        "WebScoutBot/1.0"
                );

        CrawlFetchException exception =
                assertThrows(
                        CrawlFetchException.class,
                        () -> fetcher.fetch(
                                NormalizedUrl.parse(
                                        baseUrl + "/slow"
                                ),
                                timeoutPolicy
                        )
                );

        assertEquals(
                CrawlErrorType.TIMEOUT,
                exception.errorType()
        );
    }
}