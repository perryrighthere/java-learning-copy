package com.example.kanban.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardRequest(
    @NotBlank @Size(max = 255) String name
) {
}
