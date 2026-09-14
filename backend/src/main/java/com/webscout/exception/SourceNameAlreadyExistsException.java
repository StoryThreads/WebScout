package com.webscout.exception;

public class SourceNameAlreadyExistsException extends RuntimeException {

    public SourceNameAlreadyExistsException(String name) {
        super("Source already exists with name: " + name);
    }
}