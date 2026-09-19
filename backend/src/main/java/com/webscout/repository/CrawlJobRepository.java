package com.webscout.repository;

import com.webscout.entity.CrawlJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlJobRepository extends JpaRepository<CrawlJob, Long> {
}