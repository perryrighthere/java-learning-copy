package com.example.kanban.web.dto;

import java.time.Instant;

public record CommentResponse(
    Long id,
    Long cardId,
    Long authorId,
    String authorDisplayName,
    String body,
    Instant createdAt,
    Instant updatedAt
) {
}
