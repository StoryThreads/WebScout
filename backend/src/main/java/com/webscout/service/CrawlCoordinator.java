package com.webscout.service;

import com.webscout.crawler.CrawlContext;
import com.webscout.crawler.CrawlErrorType;
import com.webscout.crawler.CrawlFetchException;
import com.webscout.crawler.CrawlPolicy;
import com.webscout.crawler.FetchPolicy;
import com.webscout.crawler.FetchResult;
import com.webscout.crawler.HtmlExtractionResult;
import com.webscout.crawler.HtmlExtractor;
import com.webscout.crawler.HttpFetcher;
import com.webscout.crawler.NormalizedUrl;
import com.webscout.crawler.RobotsPolicy;
import com.webscout.crawler.RobotsTxtPolicy;
import com.webscout.crawler.UrlFrontier;
import com.webscout.entity.CrawlJob;
import com.webscout.entity.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Service
public class CrawlCoordinator {

    private static final Logger log =
            LoggerFactory.getLogger(
                    CrawlCoordinator.class
            );

    private final CrawlJobPersistenceService
            crawlJobPersistenceService;

    private final HttpFetcher httpFetcher;

    private final HtmlExtractor htmlExtractor;

    private final WebPagePersistenceService
            webPagePersistenceService;

    private final CrawlPageResultPersistenceService
            crawlPageResultPersistenceService;

    public CrawlCoordinator(
            CrawlJobPersistenceService
                    crawlJobPersistenceService,
            HttpFetcher httpFetcher,
            HtmlExtractor htmlExtractor,
            WebPagePersistenceService
                    webPagePersistenceService,
            CrawlPageResultPersistenceService
                    crawlPageResultPersistenceService
    ) {
        this.crawlJobPersistenceService =
                crawlJobPersistenceService;

        this.httpFetcher = httpFetcher;
        this.htmlExtractor = htmlExtractor;

        this.webPagePersistenceService =
                webPagePersistenceService;

        this.crawlPageResultPersistenceService =
                crawlPageResultPersistenceService;
    }

    public void execute(Long crawlJobId) {

        Objects.requireNonNull(
                crawlJobId,
                "Crawl job id must not be null"
        );

        CrawlJob crawlJob =
                crawlJobPersistenceService
                        .getForExecution(
                                crawlJobId
                        );

        try {

            CrawlJob runningJob =
                    crawlJobPersistenceService
                            .markRunning(crawlJobId);

            runCrawl(
                    crawlJobId,
                    runningJob
            );

        } catch (Exception exception) {

            log.error(
                    "Crawl job {} failed",
                    crawlJobId,
                    exception
            );

            safelyMarkJobFailed(
                    crawlJobId,
                    exception
            );
        }
    }

    private void runCrawl(
            Long crawlJobId,
            CrawlJob crawlJob
    ) {

        Source source =
                crawlJob.getSource();

        NormalizedUrl seedUrl =
                NormalizedUrl.parse(
                        source.getBaseUrl()
                );

        CrawlPolicy crawlPolicy =
                new CrawlPolicy(
                        seedUrl.uri().getHost(),
                        source.getAllowedPathPrefix(),
                        source.getMaxPages()
                );

        FetchPolicy fetchPolicy =
                new FetchPolicy(
                        Duration.ofMillis(
                                source.getRequestTimeoutMs()
                        ),
                        5 * 1024 * 1024,
                        5,
                        source.getUserAgent()
                );

        RobotsPolicy robotsPolicy =
                new RobotsTxtPolicy(
                        seedUrl,
                        fetchPolicy.requestTimeout(),
                        fetchPolicy.userAgent(),
                        Duration.ofSeconds(
                                source.getCrawlDelaySeconds()
                        )
                );

        UrlFrontier frontier =
                new UrlFrontier();

        CrawlContext context =
                new CrawlContext(
                        crawlPolicy,
                        fetchPolicy,
                        robotsPolicy,
                        frontier
                );

        /*
         * Step 1 — seed frontier.
         */
        if (frontier.add(seedUrl)) {

            crawlJobPersistenceService
                    .incrementPagesDiscovered(
                            crawlJobId
                    );
        }

        long lastFetchNanos = 0L;

        /*
         * Step 2 — frontier loop.
         */
        while (
                !frontier.isEmpty()
                        && context.canProcessMorePages()
        ) {

            NormalizedUrl url =
                    frontier.poll();

            if (url == null) {
                break;
            }

            /*
             * Count this URL as processed by the crawl context.
             *
             * This must happen exactly once for every URL removed
             * from the frontier so that maxPages is actually enforced.
             */
            context.markPageProcessed();

            try {

                /*
                 * Scope check.
                 */
                if (!crawlPolicy.isAllowed(url)) {

                    recordSkipped(
                            crawlJobId,
                            url,
                            CrawlErrorType.OUT_OF_SCOPE.name(),
                            "URL is outside crawl scope"
                    );

                    continue;
                }

                /*
                 * robots.txt check.
                 */
                if (!robotsPolicy.isAllowed(url)) {

                    recordSkipped(
                            crawlJobId,
                            url,
                            CrawlErrorType.ROBOTS_DISALLOWED.name(),
                            "URL is disallowed by robots.txt"
                    );

                    continue;
                }

                /*
                 * Respect configured crawl delay and
                 * robots.txt crawl-delay.
                 */
                lastFetchNanos =
                        waitForCrawlDelay(
                                lastFetchNanos,
                                source,
                                robotsPolicy
                        );

                /*
                 * External HTTP call.
                 *
                 * This is intentionally NOT inside a DB
                 * transaction.
                 */
                FetchResult fetchResult =
                        httpFetcher.fetch(
                                url,
                                fetchPolicy
                        );

                /*
                 * Revalidate final redirect target
                 * against the crawl scope.
                 */
                if (!crawlPolicy.isAllowed(
                        fetchResult.finalUrl()
                )) {

                    recordSkipped(
                            crawlJobId,
                            url,
                            CrawlErrorType.OUT_OF_SCOPE.name(),
                            "Redirect target is outside crawl scope"
                    );

                    continue;
                }

                /*
                 * HTML extraction.
                 */
                HtmlExtractionResult extractionResult;

                try {

                    extractionResult =
                            htmlExtractor.extract(
                                    fetchResult.finalUrl(),
                                    new String(
                                            fetchResult.body(),
                                            StandardCharsets.UTF_8
                                    )
                            );

                } catch (RuntimeException exception) {

                    recordFailure(
                            crawlJobId,
                            url,
                            fetchResult.statusCode(),
                            fetchResult.latencyMillis(),
                            CrawlErrorType.EXTRACTION_FAILURE.name(),
                            "HTML extraction failed"
                    );

                    continue;
                }

                /*
                 * Page persistence.
                 *
                 * Short DB transaction only.
                 */
                WebPagePersistenceResult
                        persistenceResult =
                        webPagePersistenceService.persist(
                                source,
                                fetchResult.finalUrl(),
                                extractionResult,
                                fetchResult.statusCode(),
                                fetchResult.contentType()
                        );

                /*
                 * Crawl page result.
                 */
                crawlPageResultPersistenceService
                        .persistSuccess(
                                crawlJob,
                                persistenceResult,
                                url.value(),
                                fetchResult.statusCode(),
                                fetchResult.latencyMillis()
                        );

                /*
                 * Counters.
                 */
                crawlJobPersistenceService
                        .incrementPagesProcessed(
                                crawlJobId
                        );

                crawlJobPersistenceService
                        .incrementPagesSucceeded(
                                crawlJobId
                        );

                /*
                 * Link discovery.
                 */
                discoverLinks(
                        crawlJobId,
                        context,
                        extractionResult
                );

            } catch (
                    CrawlFetchException exception
            ) {

                handleFetchFailure(
                        crawlJobId,
                        url,
                        exception
                );

            } catch (RuntimeException exception) {

                /*
                 * One bad page must not kill the
                 * entire crawl.
                 */
                recordFailure(
                        crawlJobId,
                        url,
                        null,
                        0L,
                        CrawlErrorType.UNKNOWN.name(),
                        safeMessage(exception)
                );
            }
        }

        /*
         * Frontier empty or page limit reached.
         */
        crawlJobPersistenceService
                .markCompleted(
                        crawlJobId
                );

        log.info(
                "Crawl job {} completed",
                crawlJobId
        );
    }

    private void discoverLinks(
            Long crawlJobId,
            CrawlContext context,
            HtmlExtractionResult extractionResult
    ) {

        for (
                NormalizedUrl discoveredUrl
                : extractionResult.discoveredLinks()
        ) {

            if (!context.crawlPolicy()
                    .isAllowed(discoveredUrl)) {

                continue;
            }

            if (context.frontier()
                    .add(discoveredUrl)) {

                crawlJobPersistenceService
                        .incrementPagesDiscovered(
                                crawlJobId
                        );
            }
        }
    }

    private void handleFetchFailure(
            Long crawlJobId,
            NormalizedUrl url,
            CrawlFetchException exception
    ) {

        CrawlErrorType errorType =
                exception.errorType();

        /*
         * Unsupported content is not a crawler
         * infrastructure failure. It is a page that
         * cannot be processed by the HTML pipeline.
         */
        if (
                errorType
                        == CrawlErrorType.UNSUPPORTED_CONTENT_TYPE
        ) {

            recordSkipped(
                    crawlJobId,
                    url,
                    errorType.name(),
                    safeMessage(exception)
            );

            return;
        }

        recordFailure(
                crawlJobId,
                url,
                nullableStatus(
                        exception.statusCode()
                ),
                0L,
                errorType.name(),
                safeMessage(exception)
        );
    }

    private void recordSkipped(
            Long crawlJobId,
            NormalizedUrl url,
            String errorCode,
            String errorMessage
    ) {

        crawlPageResultPersistenceService
                .persistSkipped(
                        crawlJobPersistenceService
                                .getForExecution(
                                        crawlJobId
                                ),
                        url.value(),
                        0L,
                        errorCode,
                        errorMessage
                );

        crawlJobPersistenceService
                .incrementPagesProcessed(
                        crawlJobId
                );

        crawlJobPersistenceService
                .incrementPagesSkipped(
                        crawlJobId
                );
    }

    private void recordFailure(
            Long crawlJobId,
            NormalizedUrl url,
            Integer httpStatus,
            long durationMs,
            String errorCode,
            String errorMessage
    ) {

        crawlPageResultPersistenceService
                .persistFailure(
                        crawlJobPersistenceService
                                .getForExecution(
                                        crawlJobId
                                ),
                        url.value(),
                        httpStatus,
                        Math.max(
                                durationMs,
                                0L
                        ),
                        errorCode,
                        errorMessage
                );

        crawlJobPersistenceService
                .incrementPagesProcessed(
                        crawlJobId
                );

        crawlJobPersistenceService
                .incrementPagesFailed(
                        crawlJobId
                );
    }

    private void safelyMarkJobFailed(
            Long crawlJobId,
            Exception exception
    ) {

        try {

            CrawlJob current =
                    crawlJobPersistenceService
                            .getForExecution(
                                    crawlJobId
                            );

            if (CrawlJob.RUNNING.equals(
                    current.getStatus()
            )) {

                crawlJobPersistenceService
                        .markFailed(
                                crawlJobId,
                                CrawlErrorType.UNKNOWN.name(),
                                safeMessage(exception)
                        );
            }

        } catch (Exception terminalException) {

            log.error(
                    "Unable to persist terminal failure state for crawl job {}",
                    crawlJobId,
                    terminalException
            );
        }
    }

    private long waitForCrawlDelay(
            long previousFetchNanos,
            Source source,
            RobotsPolicy robotsPolicy
    ) {

        if (previousFetchNanos == 0L) {
            return System.nanoTime();
        }

        Duration configuredDelay =
                Duration.ofSeconds(
                        source.getCrawlDelaySeconds()
                );

        Duration robotsDelay =
                robotsPolicy.crawlDelay();

        Duration effectiveDelay =
                configuredDelay;

        if (
                robotsDelay != null
                        && robotsDelay.compareTo(
                        effectiveDelay
                ) > 0
        ) {
            effectiveDelay =
                    robotsDelay;
        }

        long requiredNanos =
                effectiveDelay.toNanos();

        long elapsedNanos =
                System.nanoTime()
                        - previousFetchNanos;

        long remainingNanos =
                requiredNanos - elapsedNanos;

        if (remainingNanos > 0) {

            try {

                long millis =
                        remainingNanos / 1_000_000L;

                int nanos =
                        (int)
                                (
                                        remainingNanos
                                                % 1_000_000L
                                );

                Thread.sleep(
                        millis,
                        nanos
                );

            } catch (InterruptedException exception) {

                Thread.currentThread().interrupt();

                throw new IllegalStateException(
                        "Crawl worker interrupted",
                        exception
                );
            }
        }

        return System.nanoTime();
    }

    private static Integer nullableStatus(
            int status
    ) {

        return status >= 100
                ? status
                : null;
    }

    private static String safeMessage(
            Throwable exception
    ) {

        String message =
                exception.getMessage();

        if (
                message == null
                        || message.isBlank()
        ) {
            return exception
                    .getClass()
                    .getSimpleName();
        }

        String normalized =
                message.trim();

        return normalized.length() <= 4000
                ? normalized
                : normalized.substring(0, 4000);
    }
}