package com.webscout.dto;
import jakarta.validation.constraints.*;

public class UpdateSourceRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 2048)
    private String baseUrl;

    @NotNull
    private Boolean enabled;

    @NotNull
    @Min(0)
    @Max(86400)
    private Integer crawlDelaySeconds;

    @NotNull
    @Min(1)
    @Max(120000)
    private Integer requestTimeoutMs;

    @NotNull
    @Min(1)
    @Max(10000)
    private Integer maxPages;

    @Size(max = 2048)
    private String allowedPathPrefix;

    @NotBlank
    @Size(max = 512)
    private String userAgent;

    public String getName() {
        return name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Integer getCrawlDelaySeconds() {
        return crawlDelaySeconds;
    }

    public Integer getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public Integer getMaxPages() {
        return maxPages;
    }

    public String getAllowedPathPrefix() {
        return allowedPathPrefix;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setCrawlDelaySeconds(Integer crawlDelaySeconds) {
        this.crawlDelaySeconds = crawlDelaySeconds;
    }

    public void setRequestTimeoutMs(Integer requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public void setMaxPages(Integer maxPages) {
        this.maxPages = maxPages;
    }

    public void setAllowedPathPrefix(String allowedPathPrefix) {
        this.allowedPathPrefix = allowedPathPrefix;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}