package com.webscout.crawler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrawlErrorTest {

    @Test
    void shouldCreateError() {
        CrawlError error = new CrawlError(
                CrawlErrorType.TIMEOUT,
                "Request timed out"
        );

        assertEquals(
                CrawlErrorType.TIMEOUT,
                error.type()
        );

        assertEquals(
                "Request timed out",
                error.message()
        );
    }

    @Test
    void shouldSupportEveryErrorType() {
        for (CrawlErrorType type : CrawlErrorType.values()) {
            CrawlError error = new CrawlError(
                    type,
                    "test error"
            );

            assertEquals(type, error.type());
        }
    }

    @Test
    void shouldRejectNullErrorType() {
        assertThrows(
                NullPointerException.class,
                () -> new CrawlError(
                        null,
                        "test error"
                )
        );
    }

    @Test
    void shouldRejectNullMessage() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CrawlError(
                        CrawlErrorType.UNKNOWN,
                        null
                )
        );
    }

    @Test
    void shouldRejectBlankMessage() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CrawlError(
                        CrawlErrorType.UNKNOWN,
                        "   "
                )
        );
    }
}