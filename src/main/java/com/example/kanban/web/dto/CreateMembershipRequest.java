package com.example.kanban.web.dto;

import com.example.kanban.domain.MembershipRole;
import jakarta.validation.constraints.NotNull;

public record CreateMembershipRequest(
    @NotNull Long userId,
    @NotNull MembershipRole role
) {
}
