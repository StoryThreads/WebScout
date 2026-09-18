package com.webscout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webscout.dto.CreateSourceRequest;
import com.webscout.dto.SourceResponse;
import com.webscout.dto.UpdateSourceRequest;
import com.webscout.exception.SourceNameAlreadyExistsException;
import com.webscout.exception.SourceNotFoundException;
import com.webscout.service.SourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.webscout.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SourceControllerTest {

    @Mock
    private SourceService sourceService;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        SourceController controller =
                new SourceController(sourceService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                1L,
                                null
                        )
                );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
    @Test
    void create_shouldReturnCreatedSource() throws Exception {

        CreateSourceRequest request =
                new CreateSourceRequest();

        request.setName("Example");
        request.setBaseUrl("https://example.com");
        request.setEnabled(true);
        request.setCrawlDelaySeconds(1);
        request.setRequestTimeoutMs(5000);
        request.setMaxPages(10);
        request.setAllowedPathPrefix("/");
        request.setUserAgent("WebScout/1.0");

        SourceResponse response =
                new SourceResponse();

        response.setId(10L);
        response.setUserId(1L);
        response.setName("Example");
        response.setBaseUrl("https://example.com");
        response.setEnabled(true);

        when(sourceService.create(
                eq(1L),
                any(CreateSourceRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/sources")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.name").value("Example"))
                .andExpect(jsonPath("$.baseUrl")
                        .value("https://example.com"))
                .andExpect(jsonPath("$.enabled").value(true));

        verify(sourceService)
                .create(eq(1L), any(CreateSourceRequest.class));
    }

    @Test
    void getAll_shouldReturnAuthenticatedUsersSources()
            throws Exception {

        SourceResponse source1 =
                new SourceResponse();

        source1.setId(10L);
        source1.setUserId(1L);
        source1.setName("Example");

        SourceResponse source2 =
                new SourceResponse();

        source2.setId(11L);
        source2.setUserId(1L);
        source2.setName("Example Two");

        when(sourceService.getAll(1L))
                .thenReturn(List.of(source1, source2));

        mockMvc.perform(
                        get("/api/v1/sources")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Example"))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].name")
                        .value("Example Two"));

        verify(sourceService)
                .getAll(1L);
    }

    @Test
    void getById_shouldReturnSource()
            throws Exception {

        SourceResponse response =
                new SourceResponse();

        response.setId(10L);
        response.setUserId(1L);
        response.setName("Example");

        when(sourceService.getById(1L, 10L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/v1/sources/10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.name")
                        .value("Example"));

        verify(sourceService)
                .getById(1L, 10L);
    }

    @Test
    void update_shouldReturnUpdatedSource()
            throws Exception {

        UpdateSourceRequest request =
                new UpdateSourceRequest();

        request.setName("Updated");
        request.setBaseUrl("https://updated.example.com");
        request.setEnabled(false);
        request.setCrawlDelaySeconds(2);
        request.setRequestTimeoutMs(5000);
        request.setMaxPages(20);
        request.setAllowedPathPrefix("/docs");
        request.setUserAgent("WebScout/1.0");

        SourceResponse response =
                new SourceResponse();

        response.setId(10L);
        response.setUserId(1L);
        response.setName("Updated");
        response.setBaseUrl(
                "https://updated.example.com"
        );
        response.setEnabled(false);

        when(sourceService.update(
                eq(1L),
                eq(10L),
                any(UpdateSourceRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        patch("/api/v1/sources/10")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name")
                        .value("Updated"))
                .andExpect(jsonPath("$.enabled")
                        .value(false));

        verify(sourceService)
                .update(
                        eq(1L),
                        eq(10L),
                        any(UpdateSourceRequest.class)
                );
    }

    @Test
    void delete_shouldReturnNoContent()
            throws Exception {

        doNothing()
                .when(sourceService)
                .delete(1L, 10L);

        mockMvc.perform(
                        delete("/api/v1/sources/10")
                )
                .andExpect(status().isNoContent());

        verify(sourceService)
                .delete(1L, 10L);
    }

    @Test
    void create_shouldReturnBadRequestForInvalidRequest()
            throws Exception {

        CreateSourceRequest request =
                new CreateSourceRequest();

        request.setName("");
        request.setBaseUrl("");
        request.setEnabled(null);
        request.setCrawlDelaySeconds(-1);
        request.setRequestTimeoutMs(0);
        request.setMaxPages(0);
        request.setUserAgent("");

        mockMvc.perform(
                        post("/api/v1/sources")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());

        verify(sourceService, never())
                .create(anyLong(), any());
    }

    @Test
    void create_shouldPropagateDuplicateNameException()
            throws Exception {

        CreateSourceRequest request =
                new CreateSourceRequest();

        request.setName("Example");
        request.setBaseUrl("https://example.com");
        request.setEnabled(true);
        request.setCrawlDelaySeconds(1);
        request.setRequestTimeoutMs(5000);
        request.setMaxPages(10);
        request.setUserAgent("WebScout/1.0");

        when(sourceService.create(
                eq(1L),
                any(CreateSourceRequest.class)
        )).thenThrow(
                new SourceNameAlreadyExistsException("Example")
        );

        mockMvc.perform(
                        post("/api/v1/sources")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void getById_shouldPropagateSourceNotFoundException()
            throws Exception {

        when(sourceService.getById(1L, 10L))
                .thenThrow(
                        new SourceNotFoundException(10L)
                );

        mockMvc.perform(
                        get("/api/v1/sources/10")
                )
                .andExpect(status().isNotFound());
    }
}