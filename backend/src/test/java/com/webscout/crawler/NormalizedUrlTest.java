package com.webscout.crawler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NormalizedUrlTest {

    @Test
    void shouldNormalizeSchemeAndHost() {

        NormalizedUrl url =
                NormalizedUrl.parse(
                        "HTTPS://EXAMPLE.COM/docs"
                );

        assertEquals(
                "https://example.com/docs",
                url.value()
        );
    }

    @Test
    void shouldRemoveDefaultHttpPort() {

        NormalizedUrl url =
                NormalizedUrl.parse(
                        "http://example.com:80/docs"
                );

        assertEquals(
                "http://example.com/docs",
                url.value()
        );
    }

    @Test
    void shouldRemoveDefaultHttpsPort() {

        NormalizedUrl url =
                NormalizedUrl.parse(
                        "https://example.com:443/docs"
                );

        assertEquals(
                "https://example.com/docs",
                url.value()
        );
    }

    @Test
    void shouldPreserveNonDefaultPort() {

        NormalizedUrl url =
                NormalizedUrl.parse(
                        "https://example.com:8443/docs"
                );

        assertEquals(
                "https://example.com:8443/docs",
                url.value()
        );
    }

    @Test
    void shouldRemoveFragment() {

        NormalizedUrl url =
                NormalizedUrl.parse(
                        "https://example.com/docs#installation"
                );

        assertEquals(
                "https://example.com/docs",
                url.value()
        );
    }

    @Test
    void shouldNormalizeEmptyPathToRoot() {

        NormalizedUrl url =
                NormalizedUrl.parse(
                        "https://example.com"
                );

        assertEquals(
                "https://example.com/",
                url.value()
        );
    }

    @Test
    void shouldPreserveQueryParameters() {

        NormalizedUrl url =
                NormalizedUrl.parse(
                        "https://example.com/products?id=10"
                );

        assertEquals(
                "https://example.com/products?id=10",
                url.value()
        );
    }

    @Test
    void equivalentUrlsShouldBeEqual() {

        NormalizedUrl first =
                NormalizedUrl.parse(
                        "HTTPS://EXAMPLE.COM:443/docs#section"
                );

        NormalizedUrl second =
                NormalizedUrl.parse(
                        "https://example.com/docs"
                );

        assertEquals(first, second);
        assertEquals(
                first.hashCode(),
                second.hashCode()
        );
    }

    @Test
    void shouldResolveRelativeUrl() {

        NormalizedUrl base =
                NormalizedUrl.parse(
                        "https://example.com/docs/page.html"
                );

        NormalizedUrl resolved =
                NormalizedUrl.resolve(
                        base,
                        "../guide"
                );

        assertEquals(
                "https://example.com/guide",
                resolved.value()
        );
    }

    @Test
    void shouldResolveRootRelativeUrl() {

        NormalizedUrl base =
                NormalizedUrl.parse(
                        "https://example.com/docs/page.html"
                );

        NormalizedUrl resolved =
                NormalizedUrl.resolve(
                        base,
                        "/api/v1"
                );

        assertEquals(
                "https://example.com/api/v1",
                resolved.value()
        );
    }

    @Test
    void shouldRejectUnsupportedScheme() {

        assertThrows(
                IllegalArgumentException.class,
                () -> NormalizedUrl.parse(
                        "ftp://example.com/file.txt"
                )
        );
    }

    @Test
    void shouldRejectMissingHost() {

        assertThrows(
                IllegalArgumentException.class,
                () -> NormalizedUrl.parse(
                        "https:///docs"
                )
        );
    }
}