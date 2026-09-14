package com.webscout.repository;

import com.webscout.entity.Source;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SourceRepository extends JpaRepository<Source, Long> {

    Optional<Source> findByIdAndUserId(
            Long sourceId,
            Long userId
    );

    List<Source> findAllByUserId(Long userId);

    boolean existsByUserIdAndName(
            Long userId,
            String name
    );
}