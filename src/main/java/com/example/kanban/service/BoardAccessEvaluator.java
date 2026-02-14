package com.example.kanban.service;

import com.example.kanban.auth.AuthenticatedUser;
import com.example.kanban.domain.MembershipRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("boardAccessEvaluator")
@RequiredArgsConstructor
public class BoardAccessEvaluator {

    private final MembershipService membershipService;

    public boolean canRead(Long boardId, Authentication authentication) {
        Long userId = extractUserId(authentication);
        return userId != null && membershipService.roleForUser(boardId, userId).isPresent();
    }

    public boolean canManageMembers(Long boardId, Authentication authentication) {
        Long userId = extractUserId(authentication);
        return userId != null
            && membershipService.roleForUser(boardId, userId)
            .map(role -> role == MembershipRole.ADMIN)
            .orElse(false);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return user.id();
        }
        return null;
    }
}
