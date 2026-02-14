package com.example.kanban.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
    Cors cors
) {

    public record Cors(List<String> allowedOrigins) {
    }
}
