package com.webscout.crawler;

import java.time.Duration;

public interface RobotsPolicy {

    /**
     * Determines whether the crawler may fetch the given URL.
     */
    boolean isAllowed(NormalizedUrl url);

    /**
     * Returns the crawl delay required by robots.txt.
     *
     * A null value means that robots.txt does not define
     * a crawl delay for the applicable user-agent.
     */
    Duration crawlDelay();
}