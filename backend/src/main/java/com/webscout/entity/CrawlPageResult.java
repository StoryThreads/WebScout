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

import java.time.OffsetDateTime;

@Entity
@Table(name = "crawl_page_results")
public class CrawlPageResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "crawl_job_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_crawl_page_results_job")
    )
    private CrawlJob crawlJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "page_id",
            foreignKey = @ForeignKey(name = "fk_crawl_page_results_page")
    )
    private WebPage page;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected CrawlPageResult() {
    }

    public CrawlPageResult(
            CrawlJob crawlJob,
            WebPage page,
            String url,
            String status,
            Integer httpStatus,
            Long durationMs,
            String errorCode,
            String errorMessage,
            OffsetDateTime createdAt
    ) {
        this.crawlJob = crawlJob;
        this.page = page;
        this.url = url;
        this.status = status;
        this.httpStatus = httpStatus;
        this.durationMs = durationMs;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public CrawlJob getCrawlJob() {
        return crawlJob;
    }

    public WebPage getPage() {
        return page;
    }

    public String getUrl() {
        return url;
    }

    public String getStatus() {
        return status;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}