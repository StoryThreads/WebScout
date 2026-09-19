package com.webscout.service;

import com.webscout.crawler.HtmlExtractionResult;
import com.webscout.crawler.NormalizedUrl;
import com.webscout.entity.CrawlJob;
import com.webscout.entity.CrawlPageResult;
import com.webscout.entity.Source;
import com.webscout.entity.User;
import com.webscout.repository.CrawlJobRepository;
import com.webscout.repository.CrawlPageResultRepository;
import com.webscout.repository.SourceRepository;
import com.webscout.repository.UserRepository;
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
class CrawlPersistenceIntegrationTest {

    @Autowired
    private CrawlJobPersistenceService crawlJobPersistenceService;

    @Autowired
    private CrawlPageResultPersistenceService
            crawlPageResultPersistenceService;

    @Autowired
    private WebPagePersistenceService
            webPagePersistenceService;

    @Autowired
    private CrawlJobRepository crawlJobRepository;

    @Autowired
    private CrawlPageResultRepository
            crawlPageResultRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistCompleteSuccessfulCrawlFlow() {

        Source source = createSource();

        CrawlJob crawlJob =
                crawlJobPersistenceService.create(
                        source,
                        "MANUAL"
                );

        assertThat(crawlJob.getId())
                .isNotNull();

        assertThat(crawlJob.getStatus())
                .isEqualTo("QUEUED");

        assertThat(crawlJob.getTriggerType())
                .isEqualTo("MANUAL");

        NormalizedUrl url =
                NormalizedUrl.parse(
                        "https://example.com/article"
                );

        HtmlExtractionResult extractionResult =
                new HtmlExtractionResult(
                        "Integration Test Article",
                        url,
                        "Integration test description",
                        Instant.parse(
                                "2026-09-19T10:00:00Z"
                        ),
                        "Integration test content",
                        List.of()
                );

        WebPagePersistenceResult pageResult =
                webPagePersistenceService.persist(
                        source,
                        url,
                        extractionResult,
                        200,
                        "text/html"
                );

        assertThat(pageResult.status())
                .isEqualTo(PersistenceStatus.NEW);

        CrawlPageResult crawlPageResult =
                crawlPageResultPersistenceService
                        .persistSuccess(
                                crawlJob,
                                pageResult,
                                url.value(),
                                200,
                                450L
                        );

        assertThat(crawlPageResult.getId())
                .isNotNull();

        assertThat(
                crawlPageResult
                        .getCrawlJob()
                        .getId()
        ).isEqualTo(crawlJob.getId());

        assertThat(crawlPageResult.getPage())
                .isNotNull();

        assertThat(
                crawlPageResult
                        .getPage()
                        .getId()
        ).isEqualTo(pageResult.page().getId());

        assertThat(crawlPageResult.getUrl())
                .isEqualTo(url.value());

        assertThat(crawlPageResult.getStatus())
                .isEqualTo("SUCCESS");

        assertThat(crawlPageResult.getHttpStatus())
                .isEqualTo(200);

        assertThat(crawlPageResult.getDurationMs())
                .isEqualTo(450L);

        assertThat(
                crawlPageResultRepository
                        .findById(crawlPageResult.getId())
        ).isPresent();

        assertThat(
                crawlJobRepository
                        .findById(crawlJob.getId())
        ).isPresent();
    }

    @Test
    void shouldPersistFailedPageWithoutWebPage() {

        Source source = createSource();

        CrawlJob crawlJob =
                crawlJobPersistenceService.create(
                        source,
                        "MANUAL"
                );

        CrawlPageResult result =
                crawlPageResultPersistenceService
                        .persistFailure(
                                crawlJob,
                                "https://example.com/failing-page",
                                500,
                                1200L,
                                "HTTP_ERROR",
                                "Server returned HTTP 500"
                        );

        assertThat(result.getId())
                .isNotNull();

        assertThat(result.getCrawlJob().getId())
                .isEqualTo(crawlJob.getId());

        assertThat(result.getPage())
                .isNull();

        assertThat(result.getStatus())
                .isEqualTo("FAILED");

        assertThat(result.getHttpStatus())
                .isEqualTo(500);

        assertThat(result.getDurationMs())
                .isEqualTo(1200L);

        assertThat(result.getErrorCode())
                .isEqualTo("HTTP_ERROR");

        assertThat(result.getErrorMessage())
                .isEqualTo(
                        "Server returned HTTP 500"
                );
    }

    private Source createSource() {

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        User user = new User(
                "crawl-integration-" +
                        UUID.randomUUID() +
                        "@test.com",
                "password-hash",
                "ACTIVE",
                now,
                now
        );

        user =
                userRepository.saveAndFlush(user);

        Source source = new Source(
                user,
                "Integration Test Source",
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