-- WebScout
-- V2__add_crawl_page_result_uniqueness.sql
--
-- Purpose:
-- Prevent duplicate crawl-page accounting within the same crawl job.
--
-- V1 remains immutable.

ALTER TABLE crawl_page_results
    ADD CONSTRAINT uq_crawl_page_results_job_page
        UNIQUE (crawl_job_id, page_id);

CREATE UNIQUE INDEX uq_crawl_page_results_job_url_when_page_unknown
    ON crawl_page_results (crawl_job_id, url)
    WHERE page_id IS NULL;