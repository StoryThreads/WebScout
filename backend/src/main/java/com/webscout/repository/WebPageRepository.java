package com.webscout.repository;

import com.webscout.entity.WebPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WebPageRepository extends JpaRepository<WebPage, Long> {

    @Query("""
            SELECT page
            FROM WebPage page
            WHERE page.source.id = :sourceId
              AND page.normalizedUrl = :normalizedUrl
            """)
    Optional<WebPage> findBySourceIdAndNormalizedUrl(
            @Param("sourceId") Long sourceId,
            @Param("normalizedUrl") String normalizedUrl
    );
}