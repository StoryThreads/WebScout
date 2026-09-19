package com.webscout.service;

import com.webscout.entity.CrawlJob;
import com.webscout.entity.Source;
import com.webscout.exception.ActiveCrawlAlreadyExistsException;
import com.webscout.exception.CrawlJobNotFoundException;
import com.webscout.repository.CrawlJobRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

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
        return create(
                source,
                null,
                triggerType
        );
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

        if (!CrawlJob.MANUAL.equals(triggerType)
                && !CrawlJob.SCHEDULED.equals(triggerType)) {

            throw new IllegalArgumentException(
                    "Unsupported crawl trigger type"
            );
        }

        /*
         * Fast application-level check.
         *
         * The V4 partial unique index is the actual
         * concurrency protection.
         */
        if (crawlJobRepository.existsBySourceIdAndStatusIn(
                source.getId(),
                List.of(
                        CrawlJob.QUEUED,
                        CrawlJob.RUNNING
                )
        )) {
            throw new ActiveCrawlAlreadyExistsException(
                    source.getId()
            );
        }

        CrawlJob crawlJob = new CrawlJob(
                source,
                retryOfJob,
                CrawlJob.QUEUED,
                triggerType,
                now()
        );

        try {

            return crawlJobRepository.saveAndFlush(
                    crawlJob
            );

        } catch (DataIntegrityViolationException exception) {

            /*
             * Handles the race where two requests pass
             * the exists() check simultaneously.
             */
            throw new ActiveCrawlAlreadyExistsException(
                    source.getId()
            );
        }
    }

    @Transactional
    public CrawlJob markRunning(Long jobId) {

        CrawlJob crawlJob = getRequired(jobId);

        crawlJob.markRunning(now());

        return crawlJobRepository.saveAndFlush(
                crawlJob
        );
    }

    @Transactional
    public CrawlJob markCompleted(Long jobId) {

        CrawlJob crawlJob = getRequired(jobId);

        crawlJob.markCompleted(now());

        return crawlJobRepository.saveAndFlush(
                crawlJob
        );
    }

    @Transactional
    public CrawlJob markFailed(
            Long jobId,
            String errorCode,
            String errorMessage
    ) {

        CrawlJob crawlJob = getRequired(jobId);

        crawlJob.markFailed(
                errorCode,
                errorMessage,
                now()
        );

        return crawlJobRepository.saveAndFlush(
                crawlJob
        );
    }

    @Transactional
    public CrawlJob markCancelled(Long jobId) {

        CrawlJob crawlJob = getRequired(jobId);

        crawlJob.markCancelled(now());

        return crawlJobRepository.saveAndFlush(
                crawlJob
        );
    }

    @Transactional
    public CrawlJob incrementPagesDiscovered(
            Long jobId
    ) {
        return increment(
                jobId,
                Counter.DISCOVERED
        );
    }

    @Transactional
    public CrawlJob incrementPagesProcessed(
            Long jobId
    ) {
        return increment(
                jobId,
                Counter.PROCESSED
        );
    }

    @Transactional
    public CrawlJob incrementPagesSucceeded(
            Long jobId
    ) {
        return increment(
                jobId,
                Counter.SUCCEEDED
        );
    }

    @Transactional
    public CrawlJob incrementPagesSkipped(
            Long jobId
    ) {
        return increment(
                jobId,
                Counter.SKIPPED
        );
    }

    @Transactional
    public CrawlJob incrementPagesFailed(
            Long jobId
    ) {
        return increment(
                jobId,
                Counter.FAILED
        );
    }

    @Transactional(readOnly = true)
    public CrawlJob getForExecution(
            Long jobId
    ) {

        return crawlJobRepository
                .findWithSourceById(jobId)
                .orElseThrow(
                        () -> new CrawlJobNotFoundException(
                                jobId
                        )
                );
    }

    @Transactional(readOnly = true)
    public CrawlJob getForUser(
            Long userId,
            Long jobId
    ) {

        return crawlJobRepository
                .findByIdAndUserId(
                        jobId,
                        userId
                )
                .orElseThrow(
                        () -> new CrawlJobNotFoundException(
                                jobId
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<CrawlJob> getAllForSource(
            Long userId,
            Long sourceId
    ) {

        return crawlJobRepository
                .findAllBySourceIdAndUserId(
                        sourceId,
                        userId
                );
    }

    private CrawlJob increment(
            Long jobId,
            Counter counter
    ) {

        CrawlJob crawlJob = getRequired(jobId);

        switch (counter) {

            case DISCOVERED ->
                    crawlJob.incrementPagesDiscovered();

            case PROCESSED ->
                    crawlJob.incrementPagesProcessed();

            case SUCCEEDED ->
                    crawlJob.incrementPagesSucceeded();

            case SKIPPED ->
                    crawlJob.incrementPagesSkipped();

            case FAILED ->
                    crawlJob.incrementPagesFailed();
        }

        return crawlJobRepository.saveAndFlush(
                crawlJob
        );
    }

    private CrawlJob getRequired(
            Long jobId
    ) {

        return crawlJobRepository
                .findById(jobId)
                .orElseThrow(
                        () -> new CrawlJobNotFoundException(
                                jobId
                        )
                );
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(
                ZoneOffset.UTC
        );
    }

    private enum Counter {
        DISCOVERED,
        PROCESSED,
        SUCCEEDED,
        SKIPPED,
        FAILED
    }
}