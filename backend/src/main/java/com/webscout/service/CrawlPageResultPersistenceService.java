package com.webscout.service;

import com.webscout.entity.CrawlJob;
import com.webscout.entity.CrawlPageResult;
import com.webscout.entity.WebPage;
import com.webscout.repository.CrawlPageResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class CrawlPageResultPersistenceService {

    private final CrawlPageResultRepository crawlPageResultRepository;

    public CrawlPageResultPersistenceService(
            CrawlPageResultRepository crawlPageResultRepository
    ) {
        this.crawlPageResultRepository =
                crawlPageResultRepository;
    }

    @Transactional
    public CrawlPageResult persistSuccess(
            CrawlJob crawlJob,
            WebPagePersistenceResult pageResult,
            String url,
            int httpStatus,
            long durationMs
    ) {
        if (crawlJob == null) {
            throw new IllegalArgumentException(
                    "Crawl job must not be null"
            );
        }

        if (pageResult == null) {
            throw new IllegalArgumentException(
                    "Web page persistence result must not be null"
            );
        }

        PersistenceStatus persistenceStatus =
                pageResult.status();

        String status = switch (persistenceStatus) {
            case NEW -> "SUCCESS";
            case UNCHANGED -> "UNCHANGED";
            case CHANGED -> "CHANGED";
        };

        WebPage page =
                pageResult.page();

        return persist(
                crawlJob,
                page,
                url,
                status,
                httpStatus,
                durationMs,
                null,
                null
        );
    }

    @Transactional
    public CrawlPageResult persistFailure(
            CrawlJob crawlJob,
            String url,
            Integer httpStatus,
            long durationMs,
            String errorCode,
            String errorMessage
    ) {
        if (crawlJob == null) {
            throw new IllegalArgumentException(
                    "Crawl job must not be null"
            );
        }

        return persist(
                crawlJob,
                null,
                url,
                "FAILED",
                httpStatus,
                durationMs,
                errorCode,
                errorMessage
        );
    }

    @Transactional
    public CrawlPageResult persistSkipped(
            CrawlJob crawlJob,
            String url,
            long durationMs,
            String errorCode,
            String errorMessage
    ) {
        if (crawlJob == null) {
            throw new IllegalArgumentException(
                    "Crawl job must not be null"
            );
        }

        return persist(
                crawlJob,
                null,
                url,
                "SKIPPED",
                null,
                durationMs,
                errorCode,
                errorMessage
        );
    }

    private CrawlPageResult persist(
            CrawlJob crawlJob,
            WebPage page,
            String url,
            String status,
            Integer httpStatus,
            long durationMs,
            String errorCode,
            String errorMessage
    ) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(
                    "URL must not be blank"
            );
        }

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException(
                    "Status must not be blank"
            );
        }

        if (durationMs < 0) {
            throw new IllegalArgumentException(
                    "Duration must not be negative"
            );
        }

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        CrawlPageResult result =
                new CrawlPageResult(
                        crawlJob,
                        page,
                        url,
                        status,
                        httpStatus,
                        durationMs,
                        errorCode,
                        errorMessage,
                        now
                );

        return crawlPageResultRepository.save(result);
    }
}