package com.webscout.service;

import com.webscout.entity.CrawlJob;
import com.webscout.entity.Source;
import com.webscout.exception.ActiveCrawlAlreadyExistsException;
import com.webscout.exception.CrawlJobNotFoundException;
import com.webscout.repository.CrawlJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlJobPersistenceServiceTest {

    @Mock
    private CrawlJobRepository crawlJobRepository;

    @Mock
    private Source source;

    @InjectMocks
    private CrawlJobPersistenceService persistenceService;

    private CrawlJob crawlJob;

    @BeforeEach
    void setUp() {

        crawlJob = new CrawlJob(
                source,
                null,
                CrawlJob.QUEUED,
                CrawlJob.MANUAL,
                OffsetDateTime.now()
        );
    }

    @Test
    void create_shouldCreateQueuedManualJob() {

        when(source.getId()).thenReturn(1L);

        when(
                crawlJobRepository.existsBySourceIdAndStatusIn(
                        1L,
                        List.of(
                                CrawlJob.QUEUED,
                                CrawlJob.RUNNING
                        )
                )
        ).thenReturn(false);

        when(
                crawlJobRepository.saveAndFlush(
                        any(CrawlJob.class)
                )
        ).thenReturn(crawlJob);

        CrawlJob result =
                persistenceService.create(
                        source,
                        CrawlJob.MANUAL
                );

        assertNotNull(result);
        assertEquals(
                CrawlJob.QUEUED,
                result.getStatus()
        );
        assertEquals(
                CrawlJob.MANUAL,
                result.getTriggerType()
        );

        verify(
                crawlJobRepository
        ).saveAndFlush(any(CrawlJob.class));
    }

    @Test
    void create_shouldRejectUnsupportedTriggerType() {

        assertThrows(
                IllegalArgumentException.class,
                () -> persistenceService.create(
                        source,
                        "INVALID"
                )
        );

        verifyNoInteractions(crawlJobRepository);
    }

    @Test
    void create_shouldRejectWhenActiveJobExists() {

        when(source.getId()).thenReturn(1L);

        when(
                crawlJobRepository.existsBySourceIdAndStatusIn(
                        1L,
                        List.of(
                                CrawlJob.QUEUED,
                                CrawlJob.RUNNING
                        )
                )
        ).thenReturn(true);

        assertThrows(
                ActiveCrawlAlreadyExistsException.class,
                () -> persistenceService.create(
                        source,
                        CrawlJob.MANUAL
                )
        );

        verify(
                crawlJobRepository,
                never()
        ).saveAndFlush(any());
    }

    @Test
    void create_shouldMapConcurrentInsertToActiveCrawlException() {

        when(source.getId()).thenReturn(1L);

        when(
                crawlJobRepository.existsBySourceIdAndStatusIn(
                        1L,
                        List.of(
                                CrawlJob.QUEUED,
                                CrawlJob.RUNNING
                        )
                )
        ).thenReturn(false);

        when(
                crawlJobRepository.saveAndFlush(
                        any(CrawlJob.class)
                )
        ).thenThrow(
                new DataIntegrityViolationException(
                        "unique constraint violation"
                )
        );

        assertThrows(
                ActiveCrawlAlreadyExistsException.class,
                () -> persistenceService.create(
                        source,
                        CrawlJob.MANUAL
                )
        );
    }

    @Test
    void markRunning_shouldTransitionQueuedToRunning() {

        when(
                crawlJobRepository.findWithSourceById(10L)
        ).thenReturn(Optional.of(crawlJob));

        when(
                crawlJobRepository.saveAndFlush(crawlJob)
        ).thenReturn(crawlJob);

        CrawlJob result =
                persistenceService.markRunning(10L);

        assertEquals(
                CrawlJob.RUNNING,
                result.getStatus()
        );

        assertNotNull(
                result.getStartedAt()
        );

        verify(
                crawlJobRepository
        ).findWithSourceById(10L);

        verify(
                crawlJobRepository
        ).saveAndFlush(crawlJob);
    }

    @Test
    void markCompleted_shouldTransitionRunningToCompleted() {

        crawlJob.markRunning(
                OffsetDateTime.now()
        );

        when(
                crawlJobRepository.findById(10L)
        ).thenReturn(Optional.of(crawlJob));

        when(
                crawlJobRepository.saveAndFlush(crawlJob)
        ).thenReturn(crawlJob);

        CrawlJob result =
                persistenceService.markCompleted(10L);

        assertEquals(
                CrawlJob.COMPLETED,
                result.getStatus()
        );

        assertNotNull(
                result.getFinishedAt()
        );
    }

    @Test
    void markFailed_shouldTransitionRunningToFailed() {

        crawlJob.markRunning(
                OffsetDateTime.now()
        );

        when(
                crawlJobRepository.findById(10L)
        ).thenReturn(Optional.of(crawlJob));

        when(
                crawlJobRepository.saveAndFlush(crawlJob)
        ).thenReturn(crawlJob);

        CrawlJob result =
                persistenceService.markFailed(
                        10L,
                        "CRAWL_ERROR",
                        "Crawler failed"
                );

        assertEquals(
                CrawlJob.FAILED,
                result.getStatus()
        );

        assertEquals(
                "CRAWL_ERROR",
                result.getErrorCode()
        );

        assertEquals(
                "Crawler failed",
                result.getErrorMessage()
        );

        assertNotNull(
                result.getFinishedAt()
        );
    }

    @Test
    void markCancelled_shouldCancelQueuedJob() {

        when(
                crawlJobRepository.findById(10L)
        ).thenReturn(Optional.of(crawlJob));

        when(
                crawlJobRepository.saveAndFlush(crawlJob)
        ).thenReturn(crawlJob);

        CrawlJob result =
                persistenceService.markCancelled(10L);

        assertEquals(
                CrawlJob.CANCELLED,
                result.getStatus()
        );

        assertNotNull(
                result.getFinishedAt()
        );
    }

    @Test
    void incrementPagesProcessed_shouldIncrementCounter() {

        crawlJob.markRunning(
                OffsetDateTime.now()
        );

        when(
                crawlJobRepository.findById(10L)
        ).thenReturn(Optional.of(crawlJob));

        when(
                crawlJobRepository.saveAndFlush(crawlJob)
        ).thenReturn(crawlJob);

        CrawlJob result =
                persistenceService.incrementPagesProcessed(10L);

        assertEquals(
                1,
                result.getPagesProcessed()
        );
    }

    @Test
    void incrementPagesSucceeded_shouldIncrementCounter() {

        crawlJob.markRunning(
                OffsetDateTime.now()
        );

        when(
                crawlJobRepository.findById(10L)
        ).thenReturn(Optional.of(crawlJob));

        when(
                crawlJobRepository.saveAndFlush(crawlJob)
        ).thenReturn(crawlJob);

        CrawlJob result =
                persistenceService.incrementPagesSucceeded(10L);

        assertEquals(
                1,
                result.getPagesSucceeded()
        );
    }

    @Test
    void incrementPagesSkipped_shouldIncrementCounter() {

        crawlJob.markRunning(
                OffsetDateTime.now()
        );

        when(
                crawlJobRepository.findById(10L)
        ).thenReturn(Optional.of(crawlJob));

        when(
                crawlJobRepository.saveAndFlush(crawlJob)
        ).thenReturn(crawlJob);

        CrawlJob result =
                persistenceService.incrementPagesSkipped(10L);

        assertEquals(
                1,
                result.getPagesSkipped()
        );
    }

    @Test
    void incrementPagesFailed_shouldIncrementCounter() {

        crawlJob.markRunning(
                OffsetDateTime.now()
        );

        when(
                crawlJobRepository.findById(10L)
        ).thenReturn(Optional.of(crawlJob));

        when(
                crawlJobRepository.saveAndFlush(crawlJob)
        ).thenReturn(crawlJob);

        CrawlJob result =
                persistenceService.incrementPagesFailed(10L);

        assertEquals(
                1,
                result.getPagesFailed()
        );
    }

    @Test
    void getForExecution_shouldReturnJob() {

        when(
                crawlJobRepository.findWithSourceById(10L)
        ).thenReturn(Optional.of(crawlJob));

        CrawlJob result =
                persistenceService.getForExecution(10L);

        assertSame(
                crawlJob,
                result
        );
    }

    @Test
    void getForExecution_shouldThrowWhenJobDoesNotExist() {

        when(
                crawlJobRepository.findWithSourceById(10L)
        ).thenReturn(Optional.empty());

        assertThrows(
                CrawlJobNotFoundException.class,
                () -> persistenceService.getForExecution(10L)
        );
    }
}