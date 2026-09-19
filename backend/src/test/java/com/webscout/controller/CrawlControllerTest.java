package com.webscout.controller;

import com.webscout.dto.CrawlJobResponse;
import com.webscout.service.CrawlJobService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlControllerTest {

    @Mock
    private CrawlJobService crawlJobService;

    @InjectMocks
    private CrawlController crawlController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createManualCrawl_shouldReturn202AndQueuedJob() {

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                1L,
                                null
                        )
                );

        CrawlJobResponse response =
                new CrawlJobResponse(
                        123L,
                        "QUEUED"
                );

        when(
                crawlJobService.createManualCrawl(
                        1L,
                        10L
                )
        ).thenReturn(response);

        ResponseEntity<CrawlJobResponse> result =
                crawlController.createManualCrawl(10L);

        assertEquals(
                202,
                result.getStatusCode().value()
        );

        assertNotNull(result.getBody());

        assertEquals(
                123L,
                result.getBody().crawlId()
        );

        assertEquals(
                "QUEUED",
                result.getBody().status()
        );

        verify(
                crawlJobService
        ).createManualCrawl(
                1L,
                10L
        );
    }

    @Test
    void getCrawl_shouldDelegateToService() {

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                1L,
                                null
                        )
                );

        var response =
                mock(
                        com.webscout.dto.CrawlJobDetailResponse.class
                );

        when(
                crawlJobService.getById(
                        1L,
                        20L
                )
        ).thenReturn(response);

        ResponseEntity<
                com.webscout.dto.CrawlJobDetailResponse
                > result =
                crawlController.getCrawl(20L);

        assertEquals(
                200,
                result.getStatusCode().value()
        );

        assertSame(
                response,
                result.getBody()
        );

        verify(
                crawlJobService
        ).getById(
                1L,
                20L
        );
    }

    @Test
    void getSourceCrawls_shouldDelegateToService() {

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                1L,
                                null
                        )
                );

        var response =
                java.util.List.of(
                        mock(
                                com.webscout.dto.CrawlJobDetailResponse.class
                        )
                );

        when(
                crawlJobService.getBySource(
                        1L,
                        10L
                )
        ).thenReturn(response);

        ResponseEntity<
                java.util.List<
                        com.webscout.dto.CrawlJobDetailResponse
                        >
                > result =
                crawlController.getSourceCrawls(10L);

        assertEquals(
                200,
                result.getStatusCode().value()
        );

        assertSame(
                response,
                result.getBody()
        );

        verify(
                crawlJobService
        ).getBySource(
                1L,
                10L
        );
    }

    @Test
    void createManualCrawl_withoutAuthentication_shouldThrow() {

        SecurityContextHolder.clearContext();

        assertThrows(
                IllegalStateException.class,
                () ->
                        crawlController
                                .createManualCrawl(10L)
        );

        verifyNoInteractions(
                crawlJobService
        );
    }
}