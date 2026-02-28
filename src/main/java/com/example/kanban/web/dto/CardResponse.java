package com.example.kanban.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CardResponse(
    Long id,
    Long columnId,
    String title,
    String description,
    BigDecimal position,
    Integer version,
    Instant deletedAt
) {
}
