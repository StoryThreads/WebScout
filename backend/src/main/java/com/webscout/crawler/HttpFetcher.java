package com.webscout.crawler;

public interface HttpFetcher {

    /**
     * Fetches a URL according to the supplied fetch policy.
     *
     * Implementations must not persist data directly.
     */
    FetchResult fetch(
            NormalizedUrl url,
            FetchPolicy policy
    );
}