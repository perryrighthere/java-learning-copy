package com.example.kanban.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateCardRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 4000) String description,
    @DecimalMin("0.00") BigDecimal position,
    @NotNull Integer version
) {
}
