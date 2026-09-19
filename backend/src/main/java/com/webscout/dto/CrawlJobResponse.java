package com.webscout.dto;

public record CrawlJobResponse(
        Long crawlId,
        String status
) {
}