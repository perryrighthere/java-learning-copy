package com.example.kanban;

import com.example.kanban.config.AppSecurityProperties;
import com.example.kanban.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point for the Kanban backend.
 */
@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties({JwtProperties.class, AppSecurityProperties.class})
public class KanbanApplication {

    public static void main(String[] args) {
        SpringApplication.run(KanbanApplication.class, args);
    }
}
