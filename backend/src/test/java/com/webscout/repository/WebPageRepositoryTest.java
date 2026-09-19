package com.webscout.repository;

import com.webscout.entity.Source;
import com.webscout.entity.User;
import com.webscout.entity.WebPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WebPageRepositoryTest {

    @Autowired
    private WebPageRepository webPageRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindPageBySourceIdAndNormalizedUrl() {
        OffsetDateTime now = OffsetDateTime.now();

        User user = new User(
                "webpage-repository-" + UUID.randomUUID() + "@test.com",
                "password-hash",
                "ACTIVE",
                now,
                now
        );

        user = userRepository.saveAndFlush(user);

        Source source = new Source(
                user,
                "Test Source",
                "https://example.com",
                true,
                1,
                5000,
                100,
                null,
                "WebScout-Test-Agent",
                now,
                now
        );

        source = sourceRepository.saveAndFlush(source);

        WebPage page = new WebPage(
                source,
                "https://example.com/article",
                "https://example.com/article",
                "https://example.com/article",
                "url-hash-1",
                "Test Article",
                "Test description",
                "Test content",
                "content-hash-1",
                now,
                now,
                now,
                null,
                200,
                "text/html"
        );

        webPageRepository.saveAndFlush(page);

        var result = webPageRepository.findBySourceIdAndNormalizedUrl(
                source.getId(),
                "https://example.com/article"
        );

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Test Article");
        assertThat(result.get().getNormalizedUrl())
                .isEqualTo("https://example.com/article");
    }

    @Test
    void shouldNotFindPageForDifferentNormalizedUrl() {
        OffsetDateTime now = OffsetDateTime.now();

        User user = new User(
                "different-url-" + UUID.randomUUID() + "@test.com",
                "password-hash",
                "ACTIVE",
                now,
                now
        );

        user = userRepository.saveAndFlush(user);

        Source source = new Source(
                user,
                "Test Source",
                "https://example.com",
                true,
                1,
                5000,
                100,
                null,
                "WebScout-Test-Agent",
                now,
                now
        );

        source = sourceRepository.saveAndFlush(source);

        WebPage page = new WebPage(
                source,
                "https://example.com/article",
                null,
                "https://example.com/article",
                "url-hash-2",
                "Test Article",
                null,
                "Test content",
                "content-hash-2",
                now,
                now,
                now,
                null,
                200,
                "text/html"
        );

        webPageRepository.saveAndFlush(page);

        var result = webPageRepository.findBySourceIdAndNormalizedUrl(
                source.getId(),
                "https://example.com/other"
        );

        assertThat(result).isEmpty();
    }
}