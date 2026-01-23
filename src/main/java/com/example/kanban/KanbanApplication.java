package com.example.kanban;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Kanban backend. Week 1 keeps scope minimal:
 * entities, Flyway baseline, validation, OpenAPI stub, and basic health endpoint.
 */
@SpringBootApplication
public class KanbanApplication {

    public static void main(String[] args) {
        SpringApplication.run(KanbanApplication.class, args);
    }
}
