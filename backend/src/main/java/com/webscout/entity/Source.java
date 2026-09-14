package com.webscout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "sources",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_sources_user_name",
                        columnNames = {"user_id", "name"}
                )
        }
)
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sources_user")
    )
    private User user;

    @Column(
            name = "name",
            nullable = false,
            length = 255
    )
    private String name;

    @Column(
            name = "base_url",
            nullable = false,
            length = 2048
    )
    private String baseUrl;

    @Column(
            name = "enabled",
            nullable = false
    )
    private boolean enabled;

    @Column(
            name = "crawl_delay_seconds",
            nullable = false
    )
    private int crawlDelaySeconds;

    @Column(
            name = "request_timeout_ms",
            nullable = false
    )
    private int requestTimeoutMs;

    @Column(
            name = "max_pages",
            nullable = false
    )
    private int maxPages;

    @Column(
            name = "allowed_path_prefix",
            length = 2048
    )
    private String allowedPathPrefix;

    @Column(
            name = "user_agent",
            nullable = false,
            length = 512
    )
    private String userAgent;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private OffsetDateTime updatedAt;

    protected Source() {
    }

    public Source(
            User user,
            String name,
            String baseUrl,
            boolean enabled,
            int crawlDelaySeconds,
            int requestTimeoutMs,
            int maxPages,
            String allowedPathPrefix,
            String userAgent,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.user = user;
        this.name = name;
        this.baseUrl = baseUrl;
        this.enabled = enabled;
        this.crawlDelaySeconds = crawlDelaySeconds;
        this.requestTimeoutMs = requestTimeoutMs;
        this.maxPages = maxPages;
        this.allowedPathPrefix = allowedPathPrefix;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateDetails(
            String name,
            String baseUrl,
            boolean enabled,
            int crawlDelaySeconds,
            int requestTimeoutMs,
            int maxPages,
            String allowedPathPrefix,
            String userAgent,
            OffsetDateTime updatedAt
    ) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.enabled = enabled;
        this.crawlDelaySeconds = crawlDelaySeconds;
        this.requestTimeoutMs = requestTimeoutMs;
        this.maxPages = maxPages;
        this.allowedPathPrefix = allowedPathPrefix;
        this.userAgent = userAgent;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
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
}