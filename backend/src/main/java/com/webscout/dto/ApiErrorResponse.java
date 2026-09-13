package com.webscout.dto;

import java.time.OffsetDateTime;

public class ApiErrorResponse {

    private String code;
    private String message;
    private OffsetDateTime timestamp;
    private String path;

    public ApiErrorResponse(
            String code,
            String message,
            OffsetDateTime timestamp,
            String path
    ) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
        this.path = path;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }

}