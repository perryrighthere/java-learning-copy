package com.example.kanban.web.error;

/**
 * Simple not-found marker for REST layer.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
