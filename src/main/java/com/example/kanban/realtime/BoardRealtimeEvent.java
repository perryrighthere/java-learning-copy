package com.example.kanban.realtime;

import java.time.Instant;

public record BoardRealtimeEvent(
    String eventId,
    String eventType,
    Long boardId,
    String resourceType,
    Long resourceId,
    Long resourceVersion,
    Long actorUserId,
    String originClientId,
    Instant occurredAt
) {
}
