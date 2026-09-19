package com.webscout.service;

import com.webscout.entity.CrawlJob;
import com.webscout.entity.Source;
import com.webscout.repository.CrawlJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class CrawlJobPersistenceService {

    private final CrawlJobRepository crawlJobRepository;

    public CrawlJobPersistenceService(
            CrawlJobRepository crawlJobRepository
    ) {
        this.crawlJobRepository = crawlJobRepository;
    }

    @Transactional
    public CrawlJob create(
            Source source,
            String triggerType
    ) {
        return create(source, null, triggerType);
    }

    @Transactional
    public CrawlJob create(
            Source source,
            CrawlJob retryOfJob,
            String triggerType
    ) {
        if (source == null) {
            throw new IllegalArgumentException(
                    "Source must not be null"
            );
        }

        if (triggerType == null || triggerType.isBlank()) {
            throw new IllegalArgumentException(
                    "Trigger type must not be blank"
            );
        }

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        CrawlJob crawlJob = new CrawlJob(
                source,
                retryOfJob,
                "QUEUED",
                triggerType,
                now
        );

        return crawlJobRepository.save(crawlJob);
    }
}