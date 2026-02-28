package com.example.kanban.realtime;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.realtime", name = "redis-enabled", havingValue = "true")
public class RedisBoardEventSubscriber implements MessageListener {

    private final BoardEventBus boardEventBus;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        boardEventBus.consumeRedisPayload(payload);
    }
}
