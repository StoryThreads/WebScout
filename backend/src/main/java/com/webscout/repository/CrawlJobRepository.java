package com.webscout.repository;

import com.webscout.entity.CrawlJob;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CrawlJobRepository
        extends JpaRepository<CrawlJob, Long> {

    @EntityGraph(attributePaths = "source")
    @Query("""
            SELECT job
            FROM CrawlJob job
            WHERE job.id = :jobId
            """)
    Optional<CrawlJob> findWithSourceById(
            @Param("jobId") Long jobId
    );

    @Query("""
            SELECT CASE
                WHEN COUNT(job) > 0
                THEN TRUE
                ELSE FALSE
            END
            FROM CrawlJob job
            WHERE job.source.id = :sourceId
              AND job.status IN :statuses
            """)
    boolean existsBySourceIdAndStatusIn(
            @Param("sourceId") Long sourceId,
            @Param("statuses") Collection<String> statuses
    );

    @Query("""
            SELECT job
            FROM CrawlJob job
            JOIN FETCH job.source source
            WHERE job.id = :jobId
              AND source.user.id = :userId
            """)
    Optional<CrawlJob> findByIdAndUserId(
            @Param("jobId") Long jobId,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT job
            FROM CrawlJob job
            JOIN FETCH job.source source
            WHERE source.id = :sourceId
              AND source.user.id = :userId
            ORDER BY job.createdAt DESC
            """)
    List<CrawlJob> findAllBySourceIdAndUserId(
            @Param("sourceId") Long sourceId,
            @Param("userId") Long userId
    );
}