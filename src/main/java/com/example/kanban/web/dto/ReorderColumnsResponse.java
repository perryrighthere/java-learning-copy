package com.example.kanban.web.dto;

import java.util.List;

public record ReorderColumnsResponse(
    List<ColumnResponse> columns
) {
}
