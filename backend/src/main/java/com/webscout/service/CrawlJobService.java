package com.webscout.service;

import com.webscout.dto.CrawlJobDetailResponse;
import com.webscout.dto.CrawlJobResponse;

import java.util.List;

public interface CrawlJobService {

    CrawlJobResponse createManualCrawl(
            Long userId,
            Long sourceId
    );

    CrawlJobDetailResponse getById(
            Long userId,
            Long crawlId
    );

    List<CrawlJobDetailResponse> getBySource(
            Long userId,
            Long sourceId
    );
}