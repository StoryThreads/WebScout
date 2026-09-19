package com.webscout.repository;

import com.webscout.entity.CrawlJob;
import com.webscout.entity.CrawlPageResult;
import com.webscout.entity.Source;
import com.webscout.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CrawlPageResultRepositoryTest {

    @Autowired
    private CrawlPageResultRepository crawlPageResultRepository;

    @Autowired
    private CrawlJobRepository crawlJobRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistCrawlPageResultWithoutWebPage() {

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        User user = new User(
                "crawl-result-" +
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
                "Crawl Result Test Source",
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

        source = sourceRepository.saveAndFlush(source);

        CrawlJob crawlJob = new CrawlJob(
                source,
                null,
                "RUNNING",
                "MANUAL",
                now
        );

        crawlJob =
                crawlJobRepository.saveAndFlush(crawlJob);

        CrawlPageResult result = new CrawlPageResult(
                crawlJob,
                null,
                "https://example.com/article",
                "FAILED",
                500,
                1250L,
                "HTTP_ERROR",
                "Server returned HTTP 500",
                now
        );

        CrawlPageResult saved =
                crawlPageResultRepository
                        .saveAndFlush(result);

        assertThat(saved.getId())
                .isNotNull();

        assertThat(saved.getCrawlJob().getId())
                .isEqualTo(crawlJob.getId());

        assertThat(saved.getPage())
                .isNull();

        assertThat(saved.getUrl())
                .isEqualTo(
                        "https://example.com/article"
                );

        assertThat(saved.getStatus())
                .isEqualTo("FAILED");

        assertThat(saved.getHttpStatus())
                .isEqualTo(500);

        assertThat(saved.getDurationMs())
                .isEqualTo(1250L);

        assertThat(saved.getErrorCode())
                .isEqualTo("HTTP_ERROR");

        assertThat(saved.getErrorMessage())
                .isEqualTo(
                        "Server returned HTTP 500"
                );

        assertThat(saved.getCreatedAt())
                .isEqualTo(now);
    }
}