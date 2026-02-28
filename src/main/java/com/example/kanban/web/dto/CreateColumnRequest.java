package com.example.kanban.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateColumnRequest(
    @NotBlank @Size(max = 255) String name,
    @DecimalMin("0.00") BigDecimal position
) {
}
