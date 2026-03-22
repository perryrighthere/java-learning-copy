package com.example.kanban.realtime;

import com.example.kanban.config.RealtimeProperties;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class BoardEventStreamService {

    private static final String EVENT_CONNECTED = "connected";
    private static final String EVENT_PENDING = "pending";

    private final long emitterTimeoutMs;
    private final Map<Long, Map<String, Subscriber>> subscribersByBoard = new ConcurrentHashMap<>();

    public BoardEventStreamService(RealtimeProperties realtimeProperties) {
        this.emitterTimeoutMs = realtimeProperties.sseTimeout().toMillis();
    }

    public SseEmitter subscribe(Long boardId, String clientId) {
        SseEmitter emitter = new SseEmitter(emitterTimeoutMs);
        String sessionId = UUID.randomUUID().toString();
        Subscriber subscriber = new Subscriber(sessionId, clientId, emitter);
        subscribersByBoard
            .computeIfAbsent(boardId, ignored -> new ConcurrentHashMap<>())
            .put(sessionId, subscriber);

        emitter.onCompletion(() -> remove(boardId, sessionId));
        emitter.onTimeout(() -> {
            remove(boardId, sessionId);
            emitter.complete();
        });
        emitter.onError(ex -> remove(boardId, sessionId));

        sendConnectedEvent(boardId, subscriber);
        sendPendingEvent(boardId, subscriber);
        return emitter;
    }

    public void broadcast(BoardRealtimeEvent event) {
        Map<String, Subscriber> subscribers = subscribersByBoard.get(event.boardId());
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        for (Subscriber subscriber : subscribers.values()) {
            if (shouldSuppressEcho(event, subscriber.clientId())) {
                continue;
            }
            try {
                subscriber.emitter().send(
                    SseEmitter.event()
                        .id(event.eventId())
                        .name(event.eventType())
                        .data(event)
                );
            } catch (IOException ex) {
                remove(event.boardId(), subscriber.sessionId());
                subscriber.emitter().completeWithError(ex);
            }
        }
    }

    private void sendConnectedEvent(Long boardId, Subscriber subscriber) {
        try {
            subscriber.emitter().send(
                    SseEmitter.event()
                        .id(UUID.randomUUID().toString())
                        .name(EVENT_CONNECTED)
                        .data(Map.of("boardId", boardId, "clientId", subscriber.clientId()))
            );
        } catch (IOException ex) {
            remove(boardId, subscriber.sessionId());
            subscriber.emitter().completeWithError(ex);
        }
    }

    private void sendPendingEvent(Long boardId, Subscriber subscriber) {
        try {
            subscriber.emitter().send(
                SseEmitter.event()
                    .id(UUID.randomUUID().toString())
                    .name(EVENT_PENDING)
                    .data(Map.of(
                        "boardId", boardId,
                        "clientId", subscriber.clientId(),
                        "status", "waiting_for_board_events"
                    ))
            );
        } catch (IOException ex) {
            remove(boardId, subscriber.sessionId());
            subscriber.emitter().completeWithError(ex);
        }
    }

    private boolean shouldSuppressEcho(BoardRealtimeEvent event, String subscriberClientId) {
        if (event.originClientId() == null || event.originClientId().isBlank()) {
            return false;
        }
        return event.originClientId().equals(subscriberClientId);
    }

    private void remove(Long boardId, String sessionId) {
        Map<String, Subscriber> subscribers = subscribersByBoard.get(boardId);
        if (subscribers == null) {
            return;
        }
        subscribers.remove(sessionId);
        if (subscribers.isEmpty()) {
            subscribersByBoard.remove(boardId);
        }
    }

    private record Subscriber(String sessionId, String clientId, SseEmitter emitter) {
    }
}
