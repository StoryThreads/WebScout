package com.webscout.service;

import com.webscout.entity.WebPage;

public record WebPagePersistenceResult(
        WebPage page,
        PersistenceStatus status
) {
}