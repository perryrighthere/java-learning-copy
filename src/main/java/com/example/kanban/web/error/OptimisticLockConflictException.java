package com.example.kanban.web.error;

import java.util.Map;
import java.util.List;

public class OptimisticLockConflictException extends RuntimeException {

    private static final String DEFAULT_ERROR_CODE = "optimistic_lock_conflict";
    private static final List<String> DEFAULT_GUIDANCE = List.of(
        "Fetch the latest server snapshot before retrying the write.",
        "Show the returned latest state to the user so they can confirm the change.",
        "Retry the request with the newest version and fresh ordering context."
    );

    private final Map<String, Object> latestResource;
    private final String errorCode;
    private final boolean retryable;
    private final List<String> guidance;

    public OptimisticLockConflictException(String message, Map<String, Object> latestResource) {
        this(message, latestResource, DEFAULT_ERROR_CODE, true, DEFAULT_GUIDANCE);
    }

    public OptimisticLockConflictException(String message,
                                           Map<String, Object> latestResource,
                                           String errorCode,
                                           boolean retryable,
                                           List<String> guidance) {
        super(message);
        this.latestResource = latestResource;
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.guidance = List.copyOf(guidance);
    }

    public Map<String, Object> latestResource() {
        return latestResource;
    }

    public String errorCode() {
        return errorCode;
    }

    public boolean retryable() {
        return retryable;
    }

    public List<String> guidance() {
        return guidance;
    }
}
