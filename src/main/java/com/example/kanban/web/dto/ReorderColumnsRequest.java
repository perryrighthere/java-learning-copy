package com.example.kanban.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record ReorderColumnsRequest(
    @NotEmpty List<@NotNull @Positive Long> orderedColumnIds
) {
}
