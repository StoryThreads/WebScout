package com.webscout.exception;

public class SourceNotFoundException extends RuntimeException {

    public SourceNotFoundException(Long sourceId) {
        super("Source not found: " + sourceId);
    }
}