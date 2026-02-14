package com.example.kanban.auth;

public record AuthenticatedUser(
    Long id,
    String email
) {
}
