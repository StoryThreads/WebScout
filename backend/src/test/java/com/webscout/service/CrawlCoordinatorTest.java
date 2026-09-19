package com.webscout.service;

import com.webscout.crawler.FetchResult;
import com.webscout.crawler.HtmlExtractionResult;
import com.webscout.crawler.HtmlExtractor;
import com.webscout.crawler.HttpFetcher;
import com.webscout.crawler.NormalizedUrl;
import com.webscout.entity.CrawlJob;
import com.webscout.entity.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlCoordinatorTest {

    @Mock
    private CrawlJobPersistenceService crawlJobPersistenceService;

    @Mock
    private HttpFetcher httpFetcher;

    @Mock
    private HtmlExtractor htmlExtractor;

    @Mock
    private WebPagePersistenceService webPagePersistenceService;

    @Mock
    private CrawlPageResultPersistenceService crawlPageResultPersistenceService;

    @Mock
    private Source source;

    @Mock
    private WebPagePersistenceResult webPagePersistenceResult;

    @InjectMocks
    private CrawlCoordinator crawlCoordinator;

    private CrawlJob crawlJob;

    @BeforeEach
    void setUp() {

        crawlJob =
                new CrawlJob(
                        source,
                        null,
                        CrawlJob.QUEUED,
                        CrawlJob.MANUAL,
                        OffsetDateTime.now(
                                ZoneOffset.UTC
                        )
                );
    }

    @Test
    void execute_shouldCompleteSuccessfulCrawl() {

        Long crawlJobId = 1L;

        /*
         * Source configuration required by runCrawl().
         */
        when(source.getBaseUrl())
                .thenReturn(
                        "http://127.0.0.1:1/"
                );

        when(source.getAllowedPathPrefix())
                .thenReturn("/");

        when(source.getMaxPages())
                .thenReturn(1);

        when(source.getRequestTimeoutMs())
                .thenReturn(100);

        when(source.getCrawlDelaySeconds())
                .thenReturn(0);

        when(source.getUserAgent())
                .thenReturn(
                        "WebScoutTest/1.0"
                );

        when(
                crawlJobPersistenceService
                        .getForExecution(crawlJobId)
        ).thenReturn(crawlJob);

        when(
                crawlJobPersistenceService
                        .markRunning(crawlJobId)
        ).thenAnswer(invocation -> {

            crawlJob.markRunning(
                    OffsetDateTime.now(
                            ZoneOffset.UTC
                    )
            );

            return crawlJob;
        });

        NormalizedUrl seedUrl =
                NormalizedUrl.parse(
                        "http://127.0.0.1:1/"
                );

        FetchResult fetchResult =
                FetchResult.success(
                        seedUrl,
                        seedUrl,
                        200,
                        "text/html",
                        "<html><body>Hello</body></html>"
                                .getBytes(
                                        StandardCharsets.UTF_8
                                ),
                        10L,
                        Map.of()
                );

        when(
                httpFetcher.fetch(
                        any(NormalizedUrl.class),
                        any()
                )
        ).thenReturn(fetchResult);

        HtmlExtractionResult extractionResult =
                new HtmlExtractionResult(
                        "Test Page",
                        seedUrl,
                        "Test description",
                        null,
                        "Hello",
                        List.of()
                );

        when(
                htmlExtractor.extract(
                        any(NormalizedUrl.class),
                        anyString()
                )
        ).thenReturn(
                extractionResult
        );

        when(
                webPagePersistenceService.persist(
                        eq(source),
                        any(NormalizedUrl.class),
                        eq(extractionResult),
                        eq(200),
                        eq("text/html")
                )
        ).thenReturn(
                webPagePersistenceResult
        );

        crawlCoordinator.execute(
                crawlJobId
        );

        /*
         * The coordinator delegates lifecycle persistence
         * to CrawlJobPersistenceService, so the mocked
         * CrawlJob object itself remains RUNNING.
         */
        verify(
                crawlJobPersistenceService
        ).markRunning(crawlJobId);

        verify(
                httpFetcher
        ).fetch(
                any(NormalizedUrl.class),
                any()
        );

        verify(
                htmlExtractor
        ).extract(
                any(NormalizedUrl.class),
                anyString()
        );

        verify(
                webPagePersistenceService
        ).persist(
                eq(source),
                any(NormalizedUrl.class),
                eq(extractionResult),
                eq(200),
                eq("text/html")
        );

        verify(
                crawlPageResultPersistenceService
        ).persistSuccess(
                eq(crawlJob),
                eq(webPagePersistenceResult),
                eq("http://127.0.0.1:1/"),
                eq(200),
                eq(10L)
        );

        verify(
                crawlJobPersistenceService
        ).incrementPagesDiscovered(
                crawlJobId
        );

        verify(
                crawlJobPersistenceService
        ).incrementPagesProcessed(
                crawlJobId
        );

        verify(
                crawlJobPersistenceService
        ).incrementPagesSucceeded(
                crawlJobId
        );

        verify(
                crawlJobPersistenceService
        ).markCompleted(
                crawlJobId
        );
    }

    @Test
    void execute_whenJobLookupFails_shouldPropagateException() {

        Long crawlJobId = 99L;

        RuntimeException exception =
                new RuntimeException(
                        "Job not found"
                );

        when(
                crawlJobPersistenceService
                        .getForExecution(crawlJobId)
        ).thenThrow(exception);

        RuntimeException thrown =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                crawlCoordinator.execute(
                                        crawlJobId
                                )
                );

        assertSame(
                exception,
                thrown
        );

        verify(
                crawlJobPersistenceService,
                never()
        ).markRunning(crawlJobId);

        verifyNoInteractions(
                httpFetcher,
                htmlExtractor,
                webPagePersistenceService,
                crawlPageResultPersistenceService
        );
    }

    @Test
    void execute_whenMarkRunningFails_shouldNotCallFetcher() {

        Long crawlJobId = 2L;

        when(
                crawlJobPersistenceService
                        .getForExecution(crawlJobId)
        ).thenReturn(crawlJob);

        when(
                crawlJobPersistenceService
                        .markRunning(crawlJobId)
        ).thenThrow(
                new RuntimeException(
                        "Unable to start crawl"
                )
        );

        crawlCoordinator.execute(
                crawlJobId
        );

        verify(
                crawlJobPersistenceService
        ).markRunning(crawlJobId);

        verifyNoInteractions(
                httpFetcher,
                htmlExtractor,
                webPagePersistenceService,
                crawlPageResultPersistenceService
        );
    }
}