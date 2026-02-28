package com.example.kanban.realtime;

import com.example.kanban.config.RealtimeProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardEventBus {

    private final BoardEventStreamService streamService;
    private final RealtimeProperties realtimeProperties;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public void publish(BoardRealtimeEvent event) {
        if (!realtimeProperties.redisEnabled()) {
            streamService.broadcast(event);
            return;
        }
        try {
            stringRedisTemplate.convertAndSend(channelFor(event.boardId()), objectMapper.writeValueAsString(event));
        } catch (Exception ex) {
            log.warn("Redis publish failed for board event {}, falling back to local delivery", event.eventId(), ex);
            streamService.broadcast(event);
        }
    }

    public void consumeRedisPayload(String payload) {
        try {
            BoardRealtimeEvent event = objectMapper.readValue(payload, BoardRealtimeEvent.class);
            streamService.broadcast(event);
        } catch (JsonProcessingException ex) {
            log.warn("Skipping malformed realtime payload from Redis: {}", payload, ex);
        }
    }

    String patternTopic() {
        return realtimeProperties.redisChannelPrefix() + ".*";
    }

    private String channelFor(Long boardId) {
        return realtimeProperties.redisChannelPrefix() + "." + boardId;
    }
}
