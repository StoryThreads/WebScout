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
import com.webscout.validation.SourceUrlValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class SourceServiceImpl implements SourceService {

    private final SourceRepository sourceRepository;
    private final UserRepository userRepository;
    private final SourceMapper sourceMapper;
    private final SourceValidationService sourceValidationService;

    public SourceServiceImpl(
            SourceRepository sourceRepository,
            UserRepository userRepository,
            SourceMapper sourceMapper,
            SourceValidationService sourceValidationService
    ) {
        this.sourceRepository = sourceRepository;
        this.userRepository = userRepository;
        this.sourceMapper = sourceMapper;
        this.sourceValidationService = sourceValidationService;
    }



    @Override
    public SourceResponse create(
            Long userId,
            CreateSourceRequest request
    ) {
        SourceUrlValidator.validateBaseUrl(
                request.getBaseUrl()
        );

        SourceUrlValidator.validateAllowedPathPrefix(
                request.getAllowedPathPrefix()
        );
        User user = userRepository.findById(userId)
                .orElseThrow();

        if (sourceRepository.existsByUserIdAndName(
                userId,
                request.getName()
        )) {
            throw new SourceNameAlreadyExistsException(
                    request.getName()
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        Source source = sourceMapper.toEntity(
                request,
                user,
                now
        );

        Source savedSource = sourceRepository.save(source);

        return sourceMapper.toResponse(savedSource);
    }

    @Override
    public SourceResponse update(
            Long userId,
            Long sourceId,
            UpdateSourceRequest request
    ) {
        SourceUrlValidator.validateBaseUrl(
                request.getBaseUrl()
        );

        SourceUrlValidator.validateAllowedPathPrefix(
                request.getAllowedPathPrefix()
        );
        Source source = sourceRepository
                .findByIdAndUserId(sourceId, userId)
                .orElseThrow(() ->
                        new SourceNotFoundException(sourceId)
                );

        boolean nameChanged =
                !source.getName().equals(request.getName());

        if (nameChanged &&
                sourceRepository.existsByUserIdAndName(
                        userId,
                        request.getName()
                )) {
            throw new SourceNameAlreadyExistsException(
                    request.getName()
            );
        }

        sourceMapper.updateEntity(
                source,
                request,
                OffsetDateTime.now()
        );

        return sourceMapper.toResponse(source);
    }

    @Override
    @Transactional(readOnly = true)
    public SourceResponse getById(
            Long userId,
            Long sourceId
    ) {
        Source source = sourceRepository
                .findByIdAndUserId(sourceId, userId)
                .orElseThrow(() ->
                        new SourceNotFoundException(sourceId)
                );

        return sourceMapper.toResponse(source);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SourceResponse> getAll(Long userId) {
        return sourceRepository
                .findAllByUserId(userId)
                .stream()
                .map(sourceMapper::toResponse)
                .toList();
    }

    @Override
    public void delete(
            Long userId,
            Long sourceId
    ) {
        Source source = sourceRepository
                .findByIdAndUserId(sourceId, userId)
                .orElseThrow(() ->
                        new SourceNotFoundException(sourceId)
                );

        sourceRepository.delete(source);
    }
}