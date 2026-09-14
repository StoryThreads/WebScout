package com.webscout.service;

import com.webscout.dto.CreateSourceRequest;
import com.webscout.dto.SourceResponse;
import com.webscout.dto.UpdateSourceRequest;
import com.webscout.entity.Source;
import com.webscout.entity.User;
import com.webscout.exception.SourceNameAlreadyExistsException;
import com.webscout.exception.SourceNotFoundException;
import com.webscout.mapper.SourceMapper;
import com.webscout.repository.SourceRepository;
import com.webscout.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SourceServiceImplTest {

    @Mock
    private SourceRepository sourceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SourceMapper sourceMapper;

    private SourceServiceImpl sourceService;

    @BeforeEach
    void setUp() {
        sourceService = new SourceServiceImpl(
                sourceRepository,
                userRepository,
                sourceMapper
        );
    }

    @Test
    void create_shouldCreateSourceForAuthenticatedUser() {
        Long userId = 1L;

        CreateSourceRequest request = new CreateSourceRequest();
        request.setName("Example");
        request.setBaseUrl("https://example.com");
        request.setEnabled(true);
        request.setCrawlDelaySeconds(1);
        request.setRequestTimeoutMs(5000);
        request.setMaxPages(10);
        request.setAllowedPathPrefix("/");
        request.setUserAgent("WebScout/1.0");

        User user = mock(User.class);
        Source source = mock(Source.class);
        Source savedSource = mock(Source.class);
        SourceResponse response = new SourceResponse();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(sourceRepository.existsByUserIdAndName(
                userId,
                "Example"
        )).thenReturn(false);

        when(sourceMapper.toEntity(
                eq(request),
                eq(user),
                any()
        )).thenReturn(source);

        when(sourceRepository.save(source))
                .thenReturn(savedSource);

        when(sourceMapper.toResponse(savedSource))
                .thenReturn(response);

        SourceResponse result =
                sourceService.create(userId, request);

        assertSame(response, result);

        verify(userRepository).findById(userId);

        verify(sourceRepository)
                .existsByUserIdAndName(userId, "Example");

        verify(sourceMapper)
                .toEntity(eq(request), eq(user), any());

        verify(sourceRepository).save(source);

        verify(sourceMapper).toResponse(savedSource);
    }

    @Test
    void create_shouldRejectDuplicateSourceName() {
        Long userId = 1L;

        CreateSourceRequest request = new CreateSourceRequest();
        request.setName("Example");

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(mock(User.class)));

        when(sourceRepository.existsByUserIdAndName(
                userId,
                "Example"
        )).thenReturn(true);

        assertThrows(
                SourceNameAlreadyExistsException.class,
                () -> sourceService.create(userId, request)
        );

        verify(sourceRepository)
                .existsByUserIdAndName(userId, "Example");

        verify(sourceMapper, never())
                .toEntity(any(), any(), any());

        verify(sourceRepository, never())
                .save(any());
    }

    @Test
    void update_shouldUpdateOwnedSource() {
        Long userId = 1L;
        Long sourceId = 10L;

        UpdateSourceRequest request = new UpdateSourceRequest();
        request.setName("Updated");
        request.setBaseUrl("https://updated.example.com");
        request.setEnabled(true);
        request.setCrawlDelaySeconds(2);
        request.setRequestTimeoutMs(5000);
        request.setMaxPages(20);
        request.setAllowedPathPrefix("/docs");
        request.setUserAgent("WebScout/1.0");

        Source source = mock(Source.class);
        SourceResponse response = new SourceResponse();

        when(sourceRepository.findByIdAndUserId(
                sourceId,
                userId
        )).thenReturn(Optional.of(source));

        when(source.getName())
                .thenReturn("Original");

        when(sourceMapper.toResponse(source))
                .thenReturn(response);

        SourceResponse result =
                sourceService.update(
                        userId,
                        sourceId,
                        request
                );

        assertSame(response, result);

        verify(sourceRepository)
                .findByIdAndUserId(sourceId, userId);

        verify(sourceMapper)
                .updateEntity(eq(source), eq(request), any());

        verify(sourceRepository, never())
                .save(any());
    }

    @Test
    void update_shouldRejectNonOwnedSource() {
        Long userId = 1L;
        Long sourceId = 10L;

        UpdateSourceRequest request = new UpdateSourceRequest();
        request.setName("Updated");

        when(sourceRepository.findByIdAndUserId(
                sourceId,
                userId
        )).thenReturn(Optional.empty());

        assertThrows(
                SourceNotFoundException.class,
                () -> sourceService.update(
                        userId,
                        sourceId,
                        request
                )
        );

        verify(sourceMapper, never())
                .updateEntity(any(), any(), any());

        verify(sourceRepository, never())
                .save(any());
    }

    @Test
    void update_shouldRejectDuplicateName() {
        Long userId = 1L;
        Long sourceId = 10L;

        UpdateSourceRequest request = new UpdateSourceRequest();
        request.setName("Existing");

        Source source = mock(Source.class);

        when(sourceRepository.findByIdAndUserId(
                sourceId,
                userId
        )).thenReturn(Optional.of(source));

        when(source.getName())
                .thenReturn("Original");

        when(sourceRepository.existsByUserIdAndName(
                userId,
                "Existing"
        )).thenReturn(true);

        assertThrows(
                SourceNameAlreadyExistsException.class,
                () -> sourceService.update(
                        userId,
                        sourceId,
                        request
                )
        );

        verify(sourceMapper, never())
                .updateEntity(any(), any(), any());
    }

    @Test
    void getById_shouldReturnOwnedSource() {
        Long userId = 1L;
        Long sourceId = 10L;

        Source source = mock(Source.class);
        SourceResponse response = new SourceResponse();

        when(sourceRepository.findByIdAndUserId(
                sourceId,
                userId
        )).thenReturn(Optional.of(source));

        when(sourceMapper.toResponse(source))
                .thenReturn(response);

        SourceResponse result =
                sourceService.getById(userId, sourceId);

        assertSame(response, result);

        verify(sourceRepository)
                .findByIdAndUserId(sourceId, userId);

        verify(sourceMapper)
                .toResponse(source);
    }

    @Test
    void getById_shouldThrowWhenSourceIsNotOwned() {
        Long userId = 1L;
        Long sourceId = 10L;

        when(sourceRepository.findByIdAndUserId(
                sourceId,
                userId
        )).thenReturn(Optional.empty());

        assertThrows(
                SourceNotFoundException.class,
                () -> sourceService.getById(userId, sourceId)
        );

        verify(sourceMapper, never())
                .toResponse(any());
    }

    @Test
    void getAll_shouldReturnOnlyAuthenticatedUsersSources() {
        Long userId = 1L;

        Source source1 = mock(Source.class);
        Source source2 = mock(Source.class);

        SourceResponse response1 = new SourceResponse();
        SourceResponse response2 = new SourceResponse();

        when(sourceRepository.findAllByUserId(userId))
                .thenReturn(List.of(source1, source2));

        when(sourceMapper.toResponse(source1))
                .thenReturn(response1);

        when(sourceMapper.toResponse(source2))
                .thenReturn(response2);

        List<SourceResponse> result =
                sourceService.getAll(userId);

        assertEquals(
                List.of(response1, response2),
                result
        );

        verify(sourceRepository)
                .findAllByUserId(userId);

        verify(sourceMapper)
                .toResponse(source1);

        verify(sourceMapper)
                .toResponse(source2);
    }

    @Test
    void delete_shouldDeleteOwnedSource() {
        Long userId = 1L;
        Long sourceId = 10L;

        Source source = mock(Source.class);

        when(sourceRepository.findByIdAndUserId(
                sourceId,
                userId
        )).thenReturn(Optional.of(source));

        sourceService.delete(userId, sourceId);

        verify(sourceRepository)
                .findByIdAndUserId(sourceId, userId);

        verify(sourceRepository)
                .delete(source);
    }

    @Test
    void delete_shouldThrowWhenSourceIsNotOwned() {
        Long userId = 1L;
        Long sourceId = 10L;

        when(sourceRepository.findByIdAndUserId(
                sourceId,
                userId
        )).thenReturn(Optional.empty());

        assertThrows(
                SourceNotFoundException.class,
                () -> sourceService.delete(userId, sourceId)
        );

        verify(sourceRepository, never())
                .delete(any());
    }
}