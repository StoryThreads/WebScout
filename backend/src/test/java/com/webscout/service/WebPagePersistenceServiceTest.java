package com.webscout.service;

import com.webscout.crawler.HtmlExtractionResult;
import com.webscout.crawler.NormalizedUrl;
import com.webscout.entity.Source;
import com.webscout.entity.User;
import com.webscout.entity.WebPage;
import com.webscout.repository.SourceRepository;
import com.webscout.repository.UserRepository;
import com.webscout.repository.WebPageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WebPagePersistenceServiceTest {

    @Autowired
    private WebPagePersistenceService webPagePersistenceService;

    @Autowired
    private WebPageRepository webPageRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistNewPage() {

        Source source = createSource();

        NormalizedUrl url =
                NormalizedUrl.parse(
                        "https://example.com/article"
                );

        HtmlExtractionResult extractionResult =
                new HtmlExtractionResult(
                        "Test Article",
                        url,
                        "Test description",
                        Instant.parse(
                                "2026-09-19T10:00:00Z"
                        ),
                        "Test article content",
                        List.of()
                );

        WebPagePersistenceResult result =
                webPagePersistenceService.persist(
                        source,
                        url,
                        extractionResult,
                        200,
                        "text/html"
                );

        assertThat(result.status())
                .isEqualTo(PersistenceStatus.NEW);

        assertThat(result.page())
                .isNotNull();

        WebPage page = result.page();

        assertThat(page.getId())
                .isNotNull();

        assertThat(page.getSource().getId())
                .isEqualTo(source.getId());

        assertThat(page.getOriginalUrl())
                .isEqualTo(
                        "https://example.com/article"
                );

        assertThat(page.getNormalizedUrl())
                .isEqualTo(
                        "https://example.com/article"
                );

        assertThat(page.getCanonicalUrl())
                .isEqualTo(
                        "https://example.com/article"
                );

        assertThat(page.getTitle())
                .isEqualTo("Test Article");

        assertThat(page.getDescription())
                .isEqualTo("Test description");

        assertThat(page.getContent())
                .isEqualTo("Test article content");

        assertThat(page.getContentHash())
                .isNotBlank();

        assertThat(page.getContentHash())
                .hasSize(64);

        assertThat(page.getUrlHash())
                .isNotBlank();

        assertThat(page.getUrlHash())
                .hasSize(64);

        assertThat(page.getPublishedAt())
                .isEqualTo(
                        Instant.parse(
                                "2026-09-19T10:00:00Z"
                        ).atOffset(
                                ZoneOffset.UTC
                        )
                );

        assertThat(page.getFirstDiscoveredAt())
                .isNotNull();

        assertThat(page.getLastCrawledAt())
                .isNotNull();

        assertThat(page.getLastChangedAt())
                .isNotNull();

        assertThat(page.getHttpStatus())
                .isEqualTo(200);

        assertThat(page.getContentType())
                .isEqualTo("text/html");

        assertThat(
                webPageRepository
                        .findBySourceIdAndNormalizedUrl(
                                source.getId(),
                                url.value()
                        )
        ).isPresent();
    }

    @Test
    void shouldMarkPageAsUnchangedWhenContentHashIsSame()
            throws InterruptedException {

        Source source = createSource();

        NormalizedUrl url =
                NormalizedUrl.parse(
                        "https://example.com/article"
                );

        HtmlExtractionResult firstExtraction =
                new HtmlExtractionResult(
                        "Original Title",
                        url,
                        "Original description",
                        Instant.parse(
                                "2026-09-19T10:00:00Z"
                        ),
                        "Same article content",
                        List.of()
                );

        WebPagePersistenceResult firstResult =
                webPagePersistenceService.persist(
                        source,
                        url,
                        firstExtraction,
                        200,
                        "text/html"
                );

        assertThat(firstResult.status())
                .isEqualTo(PersistenceStatus.NEW);

        WebPage firstPage = firstResult.page();

        webPageRepository.flush();

        WebPage persistedFirstPage =
                webPageRepository
                        .findById(firstPage.getId())
                        .orElseThrow();

        String originalContentHash =
                persistedFirstPage.getContentHash();

        OffsetDateTime originalLastChangedAt =
                persistedFirstPage.getLastChangedAt();

        OffsetDateTime originalLastCrawledAt =
                persistedFirstPage.getLastCrawledAt();

        Thread.sleep(10);

        HtmlExtractionResult secondExtraction =
                new HtmlExtractionResult(
                        "Updated Metadata Title",
                        url,
                        "Updated metadata description",
                        Instant.parse(
                                "2026-09-19T10:00:00Z"
                        ),
                        "Same article content",
                        List.of()
                );

        WebPagePersistenceResult secondResult =
                webPagePersistenceService.persist(
                        source,
                        url,
                        secondExtraction,
                        200,
                        "text/html"
                );

        assertThat(secondResult.status())
                .isEqualTo(PersistenceStatus.UNCHANGED);

        WebPage secondPage =
                secondResult.page();

        assertThat(secondPage.getId())
                .isEqualTo(firstPage.getId());

        assertThat(secondPage.getContent())
                .isEqualTo("Same article content");

        assertThat(secondPage.getContentHash())
                .isEqualTo(originalContentHash);

        assertThat(secondPage.getLastChangedAt())
                .isEqualTo(originalLastChangedAt);

        assertThat(secondPage.getLastCrawledAt())
                .isAfter(originalLastCrawledAt);

        assertThat(secondPage.getTitle())
                .isEqualTo(
                        "Updated Metadata Title"
                );

        assertThat(secondPage.getDescription())
                .isEqualTo(
                        "Updated metadata description"
                );

        assertThat(secondPage.getHttpStatus())
                .isEqualTo(200);

        assertThat(secondPage.getContentType())
                .isEqualTo("text/html");
    }

    @Test
    void shouldMarkPageAsChangedWhenContentHashIsDifferent() {

        Source source = createSource();

        NormalizedUrl url =
                NormalizedUrl.parse(
                        "https://example.com/article"
                );

        HtmlExtractionResult firstExtraction =
                new HtmlExtractionResult(
                        "Test Article",
                        url,
                        "Test description",
                        Instant.parse(
                                "2026-09-19T10:00:00Z"
                        ),
                        "Original article content",
                        List.of()
                );

        WebPagePersistenceResult firstResult =
                webPagePersistenceService.persist(
                        source,
                        url,
                        firstExtraction,
                        200,
                        "text/html"
                );

        assertThat(firstResult.status())
                .isEqualTo(PersistenceStatus.NEW);

        WebPage firstPage =
                firstResult.page();

        webPageRepository.flush();

        WebPage persistedFirstPage =
                webPageRepository
                        .findById(firstPage.getId())
                        .orElseThrow();

        String originalContentHash =
                persistedFirstPage.getContentHash();

        OffsetDateTime originalFirstDiscoveredAt =
                persistedFirstPage.getFirstDiscoveredAt();

        HtmlExtractionResult secondExtraction =
                new HtmlExtractionResult(
                        "Test Article",
                        url,
                        "Test description",
                        Instant.parse(
                                "2026-09-19T10:00:00Z"
                        ),
                        "Updated article content",
                        List.of()
                );

        WebPagePersistenceResult secondResult =
                webPagePersistenceService.persist(
                        source,
                        url,
                        secondExtraction,
                        200,
                        "text/html"
                );

        assertThat(secondResult.status())
                .isEqualTo(PersistenceStatus.CHANGED);

        WebPage secondPage =
                secondResult.page();

        assertThat(secondPage.getId())
                .isEqualTo(firstPage.getId());

        assertThat(secondPage.getContent())
                .isEqualTo(
                        "Updated article content"
                );

        assertThat(secondPage.getContentHash())
                .isNotEqualTo(originalContentHash);

        assertThat(secondPage.getContentHash())
                .hasSize(64);

        assertThat(secondPage.getLastChangedAt())
                .isNotNull();

        assertThat(secondPage.getLastCrawledAt())
                .isNotNull();

        assertThat(secondPage.getFirstDiscoveredAt())
                .isEqualTo(originalFirstDiscoveredAt);

        assertThat(secondPage.getTitle())
                .isEqualTo("Test Article");

        assertThat(secondPage.getDescription())
                .isEqualTo("Test description");

        assertThat(secondPage.getHttpStatus())
                .isEqualTo(200);

        assertThat(secondPage.getContentType())
                .isEqualTo("text/html");
    }

    private Source createSource() {

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        User user = new User(
                "webpage-persistence-" +
                        UUID.randomUUID() +
                        "@test.com",
                "password-hash",
                "ACTIVE",
                now,
                now
        );

        user = userRepository.saveAndFlush(user);

        Source source = new Source(
                user,
                "Test Source",
                "https://example.com",
                true,
                1,
                5000,
                100,
                null,
                "WebScout-Test-Agent",
                now,
                now
        );

        return sourceRepository.saveAndFlush(source);
    }
}