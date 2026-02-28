package com.example.kanban.realtime;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardRealtimeNotifier {

    private final BoardEventBus boardEventBus;
    private final RealtimeRequestContext realtimeRequestContext;

    public void publish(String eventType, Long boardId, String resourceType, Long resourceId) {
        publish(eventType, boardId, resourceType, resourceId, Instant.now().toEpochMilli());
    }

    public void publish(String eventType, Long boardId, String resourceType, Long resourceId, Long resourceVersion) {
        BoardRealtimeEvent event = new BoardRealtimeEvent(
            UUID.randomUUID().toString(),
            eventType,
            boardId,
            resourceType,
            resourceId,
            resourceVersion,
            realtimeRequestContext.currentUserId().orElse(null),
            realtimeRequestContext.currentClientId().orElse(null),
            Instant.now()
        );
        boardEventBus.publish(event);
    }
}
