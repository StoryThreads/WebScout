package com.webscout.service;

import com.webscout.crawler.ContentHashCalculator;
import com.webscout.crawler.HtmlExtractionResult;
import com.webscout.crawler.NormalizedUrl;
import com.webscout.entity.Source;
import com.webscout.entity.WebPage;
import com.webscout.repository.WebPageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class WebPagePersistenceService {

    private final WebPageRepository webPageRepository;

    public WebPagePersistenceService(
            WebPageRepository webPageRepository
    ) {
        this.webPageRepository = webPageRepository;
    }

    @Transactional
    public WebPagePersistenceResult persist(
            Source source,
            NormalizedUrl normalizedUrl,
            HtmlExtractionResult extractionResult,
            int httpStatus,
            String contentType
    ) {
        String normalizedContent =
                extractionResult.normalizedText();

        String contentHash =
                ContentHashCalculator.sha256(normalizedContent);

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        WebPage existingPage =
                webPageRepository
                        .findBySourceIdAndNormalizedUrl(
                                source.getId(),
                                normalizedUrl.value()
                        )
                        .orElse(null);

        /*
         * NEW PAGE
         */
        if (existingPage == null) {

            WebPage newPage = createNewPage(
                    source,
                    normalizedUrl,
                    extractionResult,
                    contentHash,
                    httpStatus,
                    contentType,
                    now
            );

            return new WebPagePersistenceResult(
                    newPage,
                    PersistenceStatus.NEW
            );
        }

        /*
         * UNCHANGED PAGE
         */
        if (existingPage.getContentHash().equals(contentHash)) {

            updateUnchangedPage(
                    existingPage,
                    extractionResult,
                    httpStatus,
                    contentType,
                    now
            );

            return new WebPagePersistenceResult(
                    existingPage,
                    PersistenceStatus.UNCHANGED
            );
        }

        /*
         * CHANGED PAGE
         */
        existingPage.updateCrawlMetadata(
                toNullableString(
                        extractionResult.canonicalUrl()
                ),
                extractionResult.title(),
                extractionResult.description(),
                extractionResult.normalizedText(),
                contentHash,
                toOffsetDateTime(
                        extractionResult.publishedAt()
                ),
                now,
                now,
                httpStatus,
                contentType
        );

        return new WebPagePersistenceResult(
                existingPage,
                PersistenceStatus.CHANGED
        );
    }

    private WebPage createNewPage(
            Source source,
            NormalizedUrl normalizedUrl,
            HtmlExtractionResult extractionResult,
            String contentHash,
            int httpStatus,
            String contentType,
            OffsetDateTime now
    ) {
        String canonicalUrl =
                toNullableString(
                        extractionResult.canonicalUrl()
                );

        OffsetDateTime publishedAt =
                toOffsetDateTime(
                        extractionResult.publishedAt()
                );

        String urlHash =
                ContentHashCalculator.sha256(
                        normalizedUrl.value()
                );

        WebPage page = new WebPage(
                source,
                normalizedUrl.value(),
                canonicalUrl,
                normalizedUrl.value(),
                urlHash,
                extractionResult.title(),
                extractionResult.description(),
                extractionResult.normalizedText(),
                contentHash,
                publishedAt,
                now,
                now,
                now,
                httpStatus,
                contentType
        );

        return webPageRepository.save(page);
    }

    private void updateUnchangedPage(
            WebPage existingPage,
            HtmlExtractionResult extractionResult,
            int httpStatus,
            String contentType,
            OffsetDateTime now
    ) {
        existingPage.updateCrawlMetadata(
                toNullableString(
                        extractionResult.canonicalUrl()
                ),
                extractionResult.title(),
                extractionResult.description(),
                extractionResult.normalizedText(),
                existingPage.getContentHash(),
                toOffsetDateTime(
                        extractionResult.publishedAt()
                ),
                now,
                existingPage.getLastChangedAt(),
                httpStatus,
                contentType
        );
    }

    private static String toNullableString(
            NormalizedUrl url
    ) {
        return url == null
                ? null
                : url.value();
    }

    private static OffsetDateTime toOffsetDateTime(
            Instant instant
    ) {
        return instant == null
                ? null
                : instant.atOffset(ZoneOffset.UTC);
    }
}