package com.webscout.service;

import com.webscout.exception.InvalidSourceException;
import com.webscout.validation.SourceUrlValidator;
import org.springframework.stereotype.Service;

@Service
public class SourceValidationService {

    public void validate(
            String baseUrl,
            String allowedPathPrefix
    ) {
        try {
            SourceUrlValidator.validateBaseUrl(baseUrl);
            SourceUrlValidator.validateAllowedPathPrefix(
                    allowedPathPrefix
            );
        } catch (IllegalArgumentException exception) {
            throw new InvalidSourceException(
                    exception.getMessage()
            );
        }
    }
}