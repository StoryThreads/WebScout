package com.webscout.dto;

import java.time.OffsetDateTime;

public class SourceResponse {

    private Long id;
    private Long userId;
    private String name;
    private String baseUrl;
    private boolean enabled;
    private int crawlDelaySeconds;
    private int requestTimeoutMs;
    private int maxPages;
    private String allowedPathPrefix;
    private String userAgent;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getCrawlDelaySeconds() {
        return crawlDelaySeconds;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public String getAllowedPathPrefix() {
        return allowedPathPrefix;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setCrawlDelaySeconds(int crawlDelaySeconds) {
        this.crawlDelaySeconds = crawlDelaySeconds;
    }

    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public void setAllowedPathPrefix(String allowedPathPrefix) {
        this.allowedPathPrefix = allowedPathPrefix;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}