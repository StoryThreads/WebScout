package com.webscout.repository;

import com.webscout.entity.CrawlPageResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlPageResultRepository
        extends JpaRepository<CrawlPageResult, Long> {
}