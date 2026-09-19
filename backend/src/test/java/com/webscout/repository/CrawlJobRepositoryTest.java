package com.webscout.repository;

import com.webscout.entity.CrawlJob;
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
class CrawlJobRepositoryTest {

    @Autowired
    private CrawlJobRepository crawlJobRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistCrawlJob() {

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        User user = new User(
                "crawl-job-" +
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
                "Crawl Job Test Source",
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
                "QUEUED",
                "MANUAL",
                now
        );

        CrawlJob saved =
                crawlJobRepository.saveAndFlush(crawlJob);

        assertThat(saved.getId())
                .isNotNull();

        assertThat(saved.getSource().getId())
                .isEqualTo(source.getId());

        assertThat(saved.getRetryOfJob())
                .isNull();

        assertThat(saved.getStatus())
                .isEqualTo("QUEUED");

        assertThat(saved.getTriggerType())
                .isEqualTo("MANUAL");

        assertThat(saved.getStartedAt())
                .isNull();

        assertThat(saved.getFinishedAt())
                .isNull();

        assertThat(saved.getPagesDiscovered())
                .isZero();

        assertThat(saved.getPagesProcessed())
                .isZero();

        assertThat(saved.getPagesSucceeded())
                .isZero();

        assertThat(saved.getPagesSkipped())
                .isZero();

        assertThat(saved.getPagesFailed())
                .isZero();

        assertThat(saved.getErrorCode())
                .isNull();

        assertThat(saved.getErrorMessage())
                .isNull();

        assertThat(saved.getCreatedAt())
                .isEqualTo(now);
    }

    @Test
    void shouldPersistRetryRelationship() {

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        User user = new User(
                "crawl-retry-" +
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
                "Retry Test Source",
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

        CrawlJob originalJob = new CrawlJob(
                source,
                null,
                "FAILED",
                "MANUAL",
                now
        );

        originalJob =
                crawlJobRepository.saveAndFlush(originalJob);

        CrawlJob retryJob = new CrawlJob(
                source,
                originalJob,
                "QUEUED",
                "MANUAL",
                now
        );

        retryJob =
                crawlJobRepository.saveAndFlush(retryJob);

        CrawlJob persistedRetry =
                crawlJobRepository
                        .findById(retryJob.getId())
                        .orElseThrow();

        assertThat(persistedRetry.getRetryOfJob())
                .isNotNull();

        assertThat(
                persistedRetry
                        .getRetryOfJob()
                        .getId()
        ).isEqualTo(originalJob.getId());
    }
}