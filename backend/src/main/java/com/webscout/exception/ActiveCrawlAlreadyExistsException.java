package com.webscout.exception;

public class ActiveCrawlAlreadyExistsException
        extends RuntimeException {

    public ActiveCrawlAlreadyExistsException(Long sourceId) {
        super(
                "An active crawl already exists for source "
                        + sourceId
        );
    }
}