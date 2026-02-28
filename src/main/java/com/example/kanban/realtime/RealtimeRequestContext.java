package com.example.kanban.realtime;

import com.example.kanban.auth.AuthenticatedUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RealtimeRequestContext {

    public static final String CLIENT_ID_HEADER = "X-Client-Id";

    public Optional<Long> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return Optional.of(user.id());
        }
        return Optional.empty();
    }

    public Optional<String> currentClientId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }
        String headerValue = attributes.getRequest().getHeader(CLIENT_ID_HEADER);
        return normalize(headerValue);
    }

    public String resolveClientId(String queryClientId, String headerClientId) {
        return normalize(queryClientId)
            .or(() -> normalize(headerClientId))
            .orElseGet(() -> "client-" + UUID.randomUUID());
    }

    private Optional<String> normalize(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
    }
}
