package com.webscout.service;

import com.webscout.exception.InvalidSourceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SourceValidationServiceTest {

    private final SourceValidationService service =
            new SourceValidationService();

    @Test
    void validate_shouldAcceptHttpUrl() {
        assertDoesNotThrow(() ->
                service.validate(
                        "http://example.com",
                        "/docs"
                )
        );
    }

    @Test
    void validate_shouldAcceptHttpsUrl() {
        assertDoesNotThrow(() ->
                service.validate(
                        "https://example.com",
                        "/docs"
                )
        );
    }

    @Test
    void validate_shouldRejectNullBaseUrl() {
        assertThrows(
                InvalidSourceException.class,
                () -> service.validate(null, "/docs")
        );
    }

    @Test
    void validate_shouldRejectBlankBaseUrl() {
        assertThrows(
                InvalidSourceException.class,
                () -> service.validate("   ", "/docs")
        );
    }

    @Test
    void validate_shouldRejectUnsupportedScheme() {
        assertThrows(
                InvalidSourceException.class,
                () -> service.validate(
                        "ftp://example.com",
                        "/docs"
                )
        );
    }

    @Test
    void validate_shouldRejectMissingHost() {
        assertThrows(
                InvalidSourceException.class,
                () -> service.validate(
                        "https:///docs",
                        "/docs"
                )
        );
    }

    @Test
    void validate_shouldRejectUserInfo() {
        assertThrows(
                InvalidSourceException.class,
                () -> service.validate(
                        "https://user:password@example.com",
                        "/docs"
                )
        );
    }

    @Test
    void validate_shouldRejectFragment() {
        assertThrows(
                InvalidSourceException.class,
                () -> service.validate(
                        "https://example.com/docs#section",
                        "/docs"
                )
        );
    }

    @Test
    void validate_shouldAcceptNullPathPrefix() {
        assertDoesNotThrow(() ->
                service.validate(
                        "https://example.com",
                        null
                )
        );
    }

    @Test
    void validate_shouldAcceptBlankPathPrefix() {
        assertDoesNotThrow(() ->
                service.validate(
                        "https://example.com",
                        "   "
                )
        );
    }

    @Test
    void validate_shouldRejectPathPrefixWithoutLeadingSlash() {
        assertThrows(
                InvalidSourceException.class,
                () -> service.validate(
                        "https://example.com",
                        "docs"
                )
        );
    }

    @Test
    void validate_shouldRejectPathPrefixContainingFragment() {
        assertThrows(
                InvalidSourceException.class,
                () -> service.validate(
                        "https://example.com",
                        "/docs#section"
                )
        );
    }
}