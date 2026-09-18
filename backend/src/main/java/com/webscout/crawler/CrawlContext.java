package com.webscout.crawler;

import java.util.Objects;

public final class CrawlContext {

    private final CrawlPolicy crawlPolicy;
    private final FetchPolicy fetchPolicy;
    private final RobotsPolicy robotsPolicy;
    private final UrlFrontier frontier;

    private int pagesProcessed;
    private boolean cancelled;

    public CrawlContext(
            CrawlPolicy crawlPolicy,
            FetchPolicy fetchPolicy,
            RobotsPolicy robotsPolicy,
            UrlFrontier frontier
    ) {
        this.crawlPolicy = Objects.requireNonNull(
                crawlPolicy,
                "Crawl policy must not be null"
        );
        this.fetchPolicy = Objects.requireNonNull(
                fetchPolicy,
                "Fetch policy must not be null"
        );
        this.robotsPolicy = Objects.requireNonNull(
                robotsPolicy,
                "Robots policy must not be null"
        );
        this.frontier = Objects.requireNonNull(
                frontier,
                "URL frontier must not be null"
        );
    }

    public CrawlPolicy crawlPolicy() {
        return crawlPolicy;
    }

    public FetchPolicy fetchPolicy() {
        return fetchPolicy;
    }

    public RobotsPolicy robotsPolicy() {
        return robotsPolicy;
    }

    public UrlFrontier frontier() {
        return frontier;
    }

    public int pagesProcessed() {
        return pagesProcessed;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean canProcessMorePages() {
        return !cancelled
                && pagesProcessed < crawlPolicy.maxPages();
    }

    public void markPageProcessed() {
        if (!canProcessMorePages()) {
            throw new IllegalStateException(
                    "Cannot process another page"
            );
        }

        pagesProcessed++;
    }

    public void cancel() {
        cancelled = true;
    }
}