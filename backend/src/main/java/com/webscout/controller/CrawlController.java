package com.webscout.controller;

import com.webscout.dto.CrawlJobDetailResponse;
import com.webscout.dto.CrawlJobResponse;
import com.webscout.service.CrawlJobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CrawlController {

    private final CrawlJobService crawlJobService;

    public CrawlController(
            CrawlJobService crawlJobService
    ) {
        this.crawlJobService =
                crawlJobService;
    }

    @PostMapping(
            "/sources/{sourceId}/crawl"
    )
    public ResponseEntity<CrawlJobResponse>
    createManualCrawl(
            @PathVariable Long sourceId
    ) {

        Long userId =
                getCurrentUserId();

        CrawlJobResponse response =
                crawlJobService
                        .createManualCrawl(
                                userId,
                                sourceId
                        );

        return ResponseEntity
                .status(
                        HttpStatus.ACCEPTED
                )
                .body(response);
    }

    @GetMapping(
            "/crawls/{crawlId}"
    )
    public ResponseEntity<CrawlJobDetailResponse>
    getCrawl(
            @PathVariable Long crawlId
    ) {

        Long userId =
                getCurrentUserId();

        return ResponseEntity.ok(
                crawlJobService.getById(
                        userId,
                        crawlId
                )
        );
    }

    @GetMapping(
            "/sources/{sourceId}/crawls"
    )
    public ResponseEntity<
            List<CrawlJobDetailResponse>
            > getSourceCrawls(
            @PathVariable Long sourceId
    ) {

        Long userId =
                getCurrentUserId();

        return ResponseEntity.ok(
                crawlJobService.getBySource(
                        userId,
                        sourceId
                )
        );
    }

    private Long getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null) {

            throw new IllegalStateException(
                    "Authentication is required"
            );
        }

        return (Long)
                authentication.getPrincipal();
    }
}