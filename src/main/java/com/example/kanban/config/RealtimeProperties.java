package com.example.kanban.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.realtime")
public record RealtimeProperties(
    boolean redisEnabled,
    String redisChannelPrefix,
    Duration sseTimeout
) {

    public RealtimeProperties {
        if (redisChannelPrefix == null || redisChannelPrefix.isBlank()) {
            redisChannelPrefix = "kanban:board-events";
        }
        if (sseTimeout == null || sseTimeout.isNegative() || sseTimeout.isZero()) {
            sseTimeout = Duration.ofMinutes(30);
        }
    }
}
