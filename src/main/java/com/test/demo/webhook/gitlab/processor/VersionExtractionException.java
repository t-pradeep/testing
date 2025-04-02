package com.test.demo.webhook.gitlab.processor;

/**
 * Custom exception for errors during version extraction from files.
 */
public class VersionExtractionException extends Exception {

    public VersionExtractionException(String message) {
        super(message);
    }

    public VersionExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
