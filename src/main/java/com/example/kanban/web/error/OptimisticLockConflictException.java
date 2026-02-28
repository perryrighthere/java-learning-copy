package com.example.kanban.web.error;

import java.util.Map;

public class OptimisticLockConflictException extends RuntimeException {

    private final Map<String, Object> latestResource;

    public OptimisticLockConflictException(String message, Map<String, Object> latestResource) {
        super(message);
        this.latestResource = latestResource;
    }

    public Map<String, Object> latestResource() {
        return latestResource;
    }
}
