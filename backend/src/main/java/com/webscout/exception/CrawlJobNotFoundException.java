package com.webscout.exception;

public class CrawlJobNotFoundException
        extends RuntimeException {

    public CrawlJobNotFoundException(Long crawlId) {
        super(
                "Crawl job not found: " + crawlId
        );
    }
}