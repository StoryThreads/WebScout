package com.webscout.service;

import com.webscout.dto.CreateSourceRequest;
import com.webscout.dto.SourceResponse;
import com.webscout.dto.UpdateSourceRequest;

import java.util.List;

public interface SourceService {

    SourceResponse create(
            Long userId,
            CreateSourceRequest request
    );

    SourceResponse update(
            Long userId,
            Long sourceId,
            UpdateSourceRequest request
    );

    SourceResponse getById(
            Long userId,
            Long sourceId
    );

    List<SourceResponse> getAll(
            Long userId
    );

    void delete(
            Long userId,
            Long sourceId
    );
}