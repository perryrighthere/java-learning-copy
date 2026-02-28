package com.example.kanban.service;

import com.example.kanban.auth.AuthenticatedUser;
import com.example.kanban.domain.MembershipRole;
import com.example.kanban.repository.BoardRepository;
import com.example.kanban.repository.BoardColumnRepository;
import com.example.kanban.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("boardAccessEvaluator")
@RequiredArgsConstructor
public class BoardAccessEvaluator {

    private final MembershipService membershipService;
    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final CardRepository cardRepository;

    public boolean canRead(Long boardId, Authentication authentication) {
        Long userId = extractUserId(authentication);
        return isOwner(boardId, userId) || hasAnyRole(boardId, userId);
    }

    public boolean canWrite(Long boardId, Authentication authentication) {
        Long userId = extractUserId(authentication);
        if (isOwner(boardId, userId)) {
            return true;
        }
        return userId != null
            && membershipService.roleForUser(boardId, userId)
            .map(role -> role == MembershipRole.ADMIN || role == MembershipRole.MEMBER)
            .orElse(false);
    }

    public boolean canManageBoard(Long boardId, Authentication authentication) {
        Long userId = extractUserId(authentication);
        if (isOwner(boardId, userId)) {
            return true;
        }
        return userId != null
            && membershipService.roleForUser(boardId, userId)
            .map(role -> role == MembershipRole.ADMIN)
            .orElse(false);
    }

    public boolean canManageMembers(Long boardId, Authentication authentication) {
        return canManageBoard(boardId, authentication);
    }

    public boolean canReadColumn(Long columnId, Authentication authentication) {
        return boardColumnRepository.findActiveBoardIdByColumnId(columnId)
            .map(boardId -> canRead(boardId, authentication))
            .orElse(false);
    }

    public boolean canWriteColumn(Long columnId, Authentication authentication) {
        return boardColumnRepository.findActiveBoardIdByColumnId(columnId)
            .map(boardId -> canWrite(boardId, authentication))
            .orElse(false);
    }

    public boolean canReadCard(Long cardId, Authentication authentication) {
        return cardRepository.findActiveBoardIdByCardId(cardId)
            .map(boardId -> canRead(boardId, authentication))
            .orElse(false);
    }

    public boolean canWriteCard(Long cardId, Authentication authentication) {
        return cardRepository.findActiveBoardIdByCardId(cardId)
            .map(boardId -> canWrite(boardId, authentication))
            .orElse(false);
    }

    private boolean hasAnyRole(Long boardId, Long userId) {
        return userId != null && membershipService.roleForUser(boardId, userId).isPresent();
    }

    private boolean isOwner(Long boardId, Long userId) {
        if (boardId == null || userId == null) {
            return false;
        }
        return boardRepository.findActiveOwnerIdById(boardId)
            .map(ownerId -> ownerId.equals(userId))
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
