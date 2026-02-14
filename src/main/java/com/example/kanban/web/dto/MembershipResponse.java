package com.example.kanban.web.dto;

import com.example.kanban.domain.MembershipRole;

public record MembershipResponse(
    Long id,
    Long userId,
    String userEmail,
    String displayName,
    MembershipRole role
) {
}
