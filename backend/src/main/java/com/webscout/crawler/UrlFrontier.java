package com.webscout.crawler;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public final class UrlFrontier {

    private final Queue<NormalizedUrl> queue = new ArrayDeque<>();
    private final Set<NormalizedUrl> scheduled = new HashSet<>();

    /**
     * Adds a URL to the frontier if it has not already been scheduled.
     *
     * @return true when the URL was newly added
     */
    public boolean add(NormalizedUrl url) {
        Objects.requireNonNull(url, "URL must not be null");

        if (!scheduled.add(url)) {
            return false;
        }

        queue.add(url);
        return true;
    }

    /**
     * Returns and removes the next URL waiting to be crawled.
     *
     * @return next URL, or null when the frontier is empty
     */
    public NormalizedUrl poll() {
        return queue.poll();
    }

    /**
     * Returns whether there are URLs waiting to be crawled.
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Returns the number of URLs currently waiting to be crawled.
     */
    public int size() {
        return queue.size();
    }

    /**
     * Returns whether this URL has already been scheduled.
     */
    public boolean contains(NormalizedUrl url) {
        Objects.requireNonNull(url, "URL must not be null");
        return scheduled.contains(url);
    }

    /**
     * Removes all queued and scheduled URLs.
     */
    public void clear() {
        queue.clear();
        scheduled.clear();
    }
}