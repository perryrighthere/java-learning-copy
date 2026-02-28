package com.example.kanban.web.dto;

import java.math.BigDecimal;

public record ColumnResponse(
    Long id,
    Long boardId,
    String name,
    BigDecimal position
) {
}
