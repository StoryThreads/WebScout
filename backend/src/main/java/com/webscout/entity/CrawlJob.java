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
@Table(name = "crawl_jobs")
public class CrawlJob {

    public static final String QUEUED = "QUEUED";
    public static final String RUNNING = "RUNNING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";
    public static final String CANCELLED = "CANCELLED";

    public static final String MANUAL = "MANUAL";
    public static final String SCHEDULED = "SCHEDULED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "source_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_crawl_jobs_source")
    )
    private Source source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "retry_of_job_id",
            foreignKey = @ForeignKey(name = "fk_crawl_jobs_retry_of")
    )
    private CrawlJob retryOfJob;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "pages_discovered", nullable = false)
    private Integer pagesDiscovered;

    @Column(name = "pages_processed", nullable = false)
    private Integer pagesProcessed;

    @Column(name = "pages_succeeded", nullable = false)
    private Integer pagesSucceeded;

    @Column(name = "pages_skipped", nullable = false)
    private Integer pagesSkipped;

    @Column(name = "pages_failed", nullable = false)
    private Integer pagesFailed;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected CrawlJob() {
    }

    public CrawlJob(
            Source source,
            CrawlJob retryOfJob,
            String status,
            String triggerType,
            OffsetDateTime createdAt
    ) {
        this.source = source;
        this.retryOfJob = retryOfJob;
        this.status = status;
        this.triggerType = triggerType;
        this.createdAt = createdAt;

        this.pagesDiscovered = 0;
        this.pagesProcessed = 0;
        this.pagesSucceeded = 0;
        this.pagesSkipped = 0;
        this.pagesFailed = 0;
    }

    public void markRunning(OffsetDateTime now) {

        requireStatus(QUEUED);

        this.status = RUNNING;
        this.startedAt = now;
        this.finishedAt = null;
        this.errorCode = null;
        this.errorMessage = null;
    }

    public void markCompleted(OffsetDateTime now) {

        requireStatus(RUNNING);

        this.status = COMPLETED;
        this.finishedAt = now;
    }

    public void markFailed(
            String errorCode,
            String errorMessage,
            OffsetDateTime now
    ) {

        requireStatus(RUNNING);

        this.status = FAILED;
        this.finishedAt = now;
        this.errorCode = normalizeErrorCode(errorCode);
        this.errorMessage = normalizeErrorMessage(errorMessage);
    }

    public void markCancelled(OffsetDateTime now) {

        if (!QUEUED.equals(status) && !RUNNING.equals(status)) {
            throw new IllegalStateException(
                    "Crawl job cannot be cancelled from status " + status
            );
        }

        this.status = CANCELLED;
        this.finishedAt = now;
    }

    public void incrementPagesDiscovered() {

        requireStatus(RUNNING);

        this.pagesDiscovered++;
    }

    public void incrementPagesProcessed() {

        requireStatus(RUNNING);

        this.pagesProcessed++;
    }

    public void incrementPagesSucceeded() {

        requireStatus(RUNNING);

        this.pagesSucceeded++;
    }

    public void incrementPagesSkipped() {

        requireStatus(RUNNING);

        this.pagesSkipped++;
    }

    public void incrementPagesFailed() {

        requireStatus(RUNNING);

        this.pagesFailed++;
    }

    private void requireStatus(String expectedStatus) {

        if (!expectedStatus.equals(status)) {
            throw new IllegalStateException(
                    "Expected crawl job status "
                            + expectedStatus
                            + " but was "
                            + status
            );
        }
    }

    private static String normalizeErrorCode(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static String normalizeErrorMessage(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        return normalized.length() <= 4000
                ? normalized
                : normalized.substring(0, 4000);
    }

    public Long getId() {
        return id;
    }

    public Source getSource() {
        return source;
    }

    public CrawlJob getRetryOfJob() {
        return retryOfJob;
    }

    public String getStatus() {
        return status;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public Integer getPagesDiscovered() {
        return pagesDiscovered;
    }

    public Integer getPagesProcessed() {
        return pagesProcessed;
    }

    public Integer getPagesSucceeded() {
        return pagesSucceeded;
    }

    public Integer getPagesSkipped() {
        return pagesSkipped;
    }

    public Integer getPagesFailed() {
        return pagesFailed;
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