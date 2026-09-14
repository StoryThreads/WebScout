package com.webscout.mapper;

import com.webscout.dto.CreateSourceRequest;
import com.webscout.dto.SourceResponse;
import com.webscout.dto.UpdateSourceRequest;
import com.webscout.entity.Source;
import com.webscout.entity.User;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class SourceMapper {

    public Source toEntity(
            CreateSourceRequest request,
            User user,
            OffsetDateTime now
    ) {
        return new Source(
                user,
                request.getName(),
                request.getBaseUrl(),
                request.getEnabled(),
                request.getCrawlDelaySeconds(),
                request.getRequestTimeoutMs(),
                request.getMaxPages(),
                request.getAllowedPathPrefix(),
                request.getUserAgent(),
                now,
                now
        );
    }

    public void updateEntity(Source source, UpdateSourceRequest request, OffsetDateTime now) {
        source.updateDetails(
                request.getName(),
                request.getBaseUrl(),
                request.getEnabled(),
                request.getCrawlDelaySeconds(),
                request.getRequestTimeoutMs(),
                request.getMaxPages(),
                request.getAllowedPathPrefix(),
                request.getUserAgent(),
                now
        );
    }

    public SourceResponse toResponse(Source source) {
        SourceResponse response = new SourceResponse();

        response.setId(source.getId());
        response.setUserId(source.getUser().getId());
        response.setName(source.getName());
        response.setBaseUrl(source.getBaseUrl());
        response.setEnabled(source.isEnabled());
        response.setCrawlDelaySeconds(source.getCrawlDelaySeconds());
        response.setRequestTimeoutMs(source.getRequestTimeoutMs());
        response.setMaxPages(source.getMaxPages());
        response.setAllowedPathPrefix(source.getAllowedPathPrefix());
        response.setUserAgent(source.getUserAgent());
        response.setCreatedAt(source.getCreatedAt());
        response.setUpdatedAt(source.getUpdatedAt());

        return response;
    }
}