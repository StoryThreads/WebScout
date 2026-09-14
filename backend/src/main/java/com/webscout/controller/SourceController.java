package com.webscout.controller;

import com.webscout.dto.CreateSourceRequest;
import com.webscout.dto.SourceResponse;
import com.webscout.dto.UpdateSourceRequest;
import com.webscout.service.SourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sources")
public class SourceController {

    private final SourceService sourceService;

    public SourceController(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @PostMapping
    public ResponseEntity<SourceResponse> create(
            @Valid @RequestBody CreateSourceRequest request
    ) {
        Long userId = getCurrentUserId();

        SourceResponse response =
                sourceService.create(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<SourceResponse>> getAll() {
        Long userId = getCurrentUserId();

        return ResponseEntity.ok(
                sourceService.getAll(userId)
        );
    }

    @GetMapping("/{sourceId}")
    public ResponseEntity<SourceResponse> getById(
            @PathVariable Long sourceId
    ) {
        Long userId = getCurrentUserId();

        return ResponseEntity.ok(
                sourceService.getById(userId, sourceId)
        );
    }

    @PutMapping("/{sourceId}")
    public ResponseEntity<SourceResponse> update(
            @PathVariable Long sourceId,
            @Valid @RequestBody UpdateSourceRequest request
    ) {
        Long userId = getCurrentUserId();

        return ResponseEntity.ok(
                sourceService.update(
                        userId,
                        sourceId,
                        request
                )
        );
    }

    @DeleteMapping("/{sourceId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long sourceId
    ) {
        Long userId = getCurrentUserId();

        sourceService.delete(userId, sourceId);

        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        return (Long) authentication.getPrincipal();
    }
}