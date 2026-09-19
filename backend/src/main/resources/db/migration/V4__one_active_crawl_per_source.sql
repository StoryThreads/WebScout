-- WebScout
-- Phase 9
--
-- Guarantee that a source can have at most one
-- QUEUED or RUNNING crawl at a time.

CREATE UNIQUE INDEX uq_crawl_jobs_one_active_per_source
    ON crawl_jobs (source_id)
    WHERE status IN ('QUEUED', 'RUNNING');