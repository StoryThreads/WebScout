package com.webscout.service;

import com.webscout.dto.CrawlJobDetailResponse;
import com.webscout.dto.CrawlJobResponse;
import com.webscout.entity.CrawlJob;
import com.webscout.entity.Source;
import com.webscout.exception.InvalidSourceException;
import com.webscout.exception.SourceNotFoundException;
import com.webscout.repository.SourceRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;

@Service
public class CrawlJobServiceImpl
        implements CrawlJobService {

    private final SourceRepository sourceRepository;
    private final CrawlJobPersistenceService
            crawlJobPersistenceService;
    private final CrawlCoordinator crawlCoordinator;
    private final Executor crawlTaskExecutor;

    public CrawlJobServiceImpl(
            SourceRepository sourceRepository,
            CrawlJobPersistenceService
                    crawlJobPersistenceService,
            CrawlCoordinator crawlCoordinator,
            @Qualifier("crawlTaskExecutor")
            Executor crawlTaskExecutor
    ) {
        this.sourceRepository =
                sourceRepository;

        this.crawlJobPersistenceService =
                crawlJobPersistenceService;

        this.crawlCoordinator =
                crawlCoordinator;

        this.crawlTaskExecutor =
                crawlTaskExecutor;
    }

    @Override
    public CrawlJobResponse createManualCrawl(
            Long userId,
            Long sourceId
    ) {

        Source source =
                sourceRepository
                        .findByIdAndUserId(
                                sourceId,
                                userId
                        )
                        .orElseThrow(
                                () ->
                                        new SourceNotFoundException(
                                                sourceId
                                        )
                        );

        if (!source.isEnabled()) {

            throw new InvalidSourceException(
                    "Source is disabled and cannot be crawled"
            );
        }

        CrawlJob crawlJob =
                crawlJobPersistenceService.create(
                        source,
                        CrawlJob.MANUAL
                );

        /*
         * The API request ends here.
         *
         * The actual crawler runs asynchronously.
         */
        crawlTaskExecutor.execute(
                () ->
                        crawlCoordinator.execute(
                                crawlJob.getId()
                        )
        );

        return new CrawlJobResponse(
                crawlJob.getId(),
                crawlJob.getStatus()
        );
    }

    @Override
    public CrawlJobDetailResponse getById(
            Long userId,
            Long crawlId
    ) {

        return toDetailResponse(
                crawlJobPersistenceService.getForUser(
                        userId,
                        crawlId
                )
        );
    }

    @Override
    public List<CrawlJobDetailResponse> getBySource(
            Long userId,
            Long sourceId
    ) {

        sourceRepository
                .findByIdAndUserId(
                        sourceId,
                        userId
                )
                .orElseThrow(
                        () ->
                                new SourceNotFoundException(
                                        sourceId
                                )
                );

        return crawlJobPersistenceService
                .getAllForSource(
                        userId,
                        sourceId
                )
                .stream()
                .map(
                        CrawlJobServiceImpl::toDetailResponse
                )
                .toList();
    }

    private static CrawlJobDetailResponse
    toDetailResponse(
            CrawlJob crawlJob
    ) {

        return new CrawlJobDetailResponse(
                crawlJob.getId(),
                crawlJob.getStatus(),
                crawlJob.getTriggerType(),
                crawlJob.getStartedAt(),
                crawlJob.getFinishedAt(),
                crawlJob.getPagesDiscovered(),
                crawlJob.getPagesProcessed(),
                crawlJob.getPagesSucceeded(),
                crawlJob.getPagesSkipped(),
                crawlJob.getPagesFailed(),
                crawlJob.getErrorCode(),
                crawlJob.getErrorMessage(),
                crawlJob.getCreatedAt()
        );
    }
}