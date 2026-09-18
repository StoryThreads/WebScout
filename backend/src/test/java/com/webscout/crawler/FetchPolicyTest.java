package com.webscout.crawler;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class FetchPolicyTest {

    @Test
    void shouldCreatePolicyWithValidValues() {
        FetchPolicy policy = new FetchPolicy(
                Duration.ofSeconds(10),
                5_000_000L,
                5,
                "WebScoutBot/1.0"
        );

        assertEquals(Duration.ofSeconds(10), policy.requestTimeout());
        assertEquals(5_000_000L, policy.maxResponseBytes());
        assertEquals(5, policy.maxRedirects());
        assertEquals("WebScoutBot/1.0", policy.userAgent());
    }

    @Test
    void shouldRejectNullTimeout() {
        assertThrows(
                NullPointerException.class,
                () -> new FetchPolicy(
                        null,
                        1_000,
                        5,
                        "WebScoutBot/1.0"
                )
        );
    }

    @Test
    void shouldRejectZeroTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FetchPolicy(
                        Duration.ZERO,
                        1_000,
                        5,
                        "WebScoutBot/1.0"
                )
        );
    }

    @Test
    void shouldRejectNegativeTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FetchPolicy(
                        Duration.ofSeconds(-1),
                        1_000,
                        5,
                        "WebScoutBot/1.0"
                )
        );
    }

    @Test
    void shouldRejectZeroResponseLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FetchPolicy(
                        Duration.ofSeconds(10),
                        0,
                        5,
                        "WebScoutBot/1.0"
                )
        );
    }

    @Test
    void shouldRejectNegativeResponseLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FetchPolicy(
                        Duration.ofSeconds(10),
                        -1,
                        5,
                        "WebScoutBot/1.0"
                )
        );
    }

    @Test
    void shouldAllowZeroRedirects() {
        FetchPolicy policy = new FetchPolicy(
                Duration.ofSeconds(10),
                1_000,
                0,
                "WebScoutBot/1.0"
        );

        assertEquals(0, policy.maxRedirects());
    }

    @Test
    void shouldRejectNegativeRedirectLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FetchPolicy(
                        Duration.ofSeconds(10),
                        1_000,
                        -1,
                        "WebScoutBot/1.0"
                )
        );
    }

    @Test
    void shouldRejectNullUserAgent() {
        assertThrows(
                NullPointerException.class,
                () -> new FetchPolicy(
                        Duration.ofSeconds(10),
                        1_000,
                        5,
                        null
                )
        );
    }

    @Test
    void shouldRejectBlankUserAgent() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FetchPolicy(
                        Duration.ofSeconds(10),
                        1_000,
                        5,
                        "   "
                )
        );
    }
}