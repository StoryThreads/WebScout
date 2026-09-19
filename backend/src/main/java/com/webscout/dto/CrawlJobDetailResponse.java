package com.webscout.dto;

import java.time.OffsetDateTime;

public record CrawlJobDetailResponse(
        Long crawlId,
        String status,
        String triggerType,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        Integer pagesDiscovered,
        Integer pagesProcessed,
        Integer pagesSucceeded,
        Integer pagesSkipped,
        Integer pagesFailed,
        String errorCode,
        String errorMessage,
        OffsetDateTime createdAt
) {
}