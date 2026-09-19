package com.webscout.crawler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpStatusClassifierTest {

    @Test
    void shouldClassifySuccessfulResponses() {

        assertNull(
                HttpStatusClassifier.classify(200)
        );

        assertNull(
                HttpStatusClassifier.classify(204)
        );

        assertTrue(
                HttpStatusClassifier.isSuccessful(200)
        );
    }

    @Test
    void shouldClassifyRedirectResponses() {

        assertEquals(
                CrawlErrorType.HTTP_REDIRECTION_ERROR,
                HttpStatusClassifier.classify(301)
        );

        assertEquals(
                CrawlErrorType.HTTP_REDIRECTION_ERROR,
                HttpStatusClassifier.classify(302)
        );
    }

    @Test
    void shouldClassifyClientErrors() {

        assertEquals(
                CrawlErrorType.HTTP_CLIENT_ERROR,
                HttpStatusClassifier.classify(400)
        );

        assertEquals(
                CrawlErrorType.HTTP_CLIENT_ERROR,
                HttpStatusClassifier.classify(404)
        );
    }

    @Test
    void shouldClassifyRateLimit() {

        assertEquals(
                CrawlErrorType.HTTP_RATE_LIMITED,
                HttpStatusClassifier.classify(429)
        );
    }

    @Test
    void shouldClassifyServerErrors() {

        assertEquals(
                CrawlErrorType.HTTP_SERVER_ERROR,
                HttpStatusClassifier.classify(500)
        );

        assertEquals(
                CrawlErrorType.HTTP_SERVER_ERROR,
                HttpStatusClassifier.classify(503)
        );
    }

    @Test
    void shouldRejectInvalidStatusCodes() {

        assertThrows(
                IllegalArgumentException.class,
                () -> HttpStatusClassifier.classify(99)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> HttpStatusClassifier.classify(600)
        );
    }
}