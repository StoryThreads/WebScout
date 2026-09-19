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
        name = "web_pages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_web_pages_source_url_hash",
                        columnNames = {"source_id", "url_hash"}
                )
        }
)
public class WebPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "source_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_web_pages_source")
    )
    private Source source;

    @Column(
            name = "original_url",
            nullable = false,
            length = 2048
    )
    private String originalUrl;

    @Column(
            name = "canonical_url",
            length = 2048
    )
    private String canonicalUrl;

    @Column(
            name = "normalized_url",
            nullable = false,
            length = 2048
    )
    private String normalizedUrl;

    @Column(
            name = "url_hash",
            nullable = false,
            length = 64
    )
    private String urlHash;

    @Column(
            name = "title",
            length = 1000
    )
    private String title;

    @Column(
            name = "description",
            columnDefinition = "TEXT"
    )
    private String description;

    @Column(
            name = "content",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String content;

    @Column(
            name = "content_hash",
            nullable = false,
            length = 64
    )
    private String contentHash;

    @Column(
            name = "published_at"
    )
    private OffsetDateTime publishedAt;

    @Column(
            name = "first_discovered_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime firstDiscoveredAt;

    @Column(
            name = "last_crawled_at",
            nullable = false
    )
    private OffsetDateTime lastCrawledAt;

    @Column(
            name = "last_changed_at"
    )
    private OffsetDateTime lastChangedAt;

    @Column(
            name = "http_status"
    )
    private Integer httpStatus;

    @Column(
            name = "content_type",
            length = 255
    )
    private String contentType;

    protected WebPage() {
    }

    public void updateCrawlMetadata(
            String canonicalUrl,
            String title,
            String description,
            String content,
            String contentHash,
            OffsetDateTime publishedAt,
            OffsetDateTime lastCrawledAt,
            OffsetDateTime lastChangedAt,
            Integer httpStatus,
            String contentType
    ) {
        this.canonicalUrl = canonicalUrl;
        this.title = title;
        this.description = description;
        this.content = content;
        this.contentHash = contentHash;
        this.publishedAt = publishedAt;
        this.lastCrawledAt = lastCrawledAt;
        this.lastChangedAt = lastChangedAt;
        this.httpStatus = httpStatus;
        this.contentType = contentType;
    }

    public WebPage(
            Source source,
            String originalUrl,
            String canonicalUrl,
            String normalizedUrl,
            String urlHash,
            String title,
            String description,
            String content,
            String contentHash,
            OffsetDateTime publishedAt,
            OffsetDateTime firstDiscoveredAt,
            OffsetDateTime lastCrawledAt,
            OffsetDateTime lastChangedAt,
            Integer httpStatus,
            String contentType
    ) {
        this.source = source;
        this.originalUrl = originalUrl;
        this.canonicalUrl = canonicalUrl;
        this.normalizedUrl = normalizedUrl;
        this.urlHash = urlHash;
        this.title = title;
        this.description = description;
        this.content = content;
        this.contentHash = contentHash;
        this.publishedAt = publishedAt;
        this.firstDiscoveredAt = firstDiscoveredAt;
        this.lastCrawledAt = lastCrawledAt;
        this.lastChangedAt = lastChangedAt;
        this.httpStatus = httpStatus;
        this.contentType = contentType;
    }

    public Long getId() {
        return id;
    }

    public Source getSource() {
        return source;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    public String getUrlHash() {
        return urlHash;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getContent() {
        return content;
    }

    public String getContentHash() {
        return contentHash;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public OffsetDateTime getFirstDiscoveredAt() {
        return firstDiscoveredAt;
    }

    public OffsetDateTime getLastCrawledAt() {
        return lastCrawledAt;
    }

    public OffsetDateTime getLastChangedAt() {
        return lastChangedAt;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getContentType() {
        return contentType;
    }
}