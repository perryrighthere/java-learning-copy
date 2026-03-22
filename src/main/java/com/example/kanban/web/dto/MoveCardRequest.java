package com.example.kanban.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MoveCardRequest(
    @NotNull @Positive Long targetColumnId,
    @Positive Long previousCardId,
    @Positive Long nextCardId,
    @NotNull Integer version
) {
}
