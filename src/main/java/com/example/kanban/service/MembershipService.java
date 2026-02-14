package com.example.kanban.service;

import com.example.kanban.domain.Board;
import com.example.kanban.domain.Membership;
import com.example.kanban.domain.MembershipRole;
import com.example.kanban.domain.User;
import com.example.kanban.repository.BoardRepository;
import com.example.kanban.repository.MembershipRepository;
import com.example.kanban.web.dto.CreateMembershipRequest;
import com.example.kanban.web.error.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final BoardRepository boardRepository;
    private final UserService userService;

    public List<Membership> listByBoard(Long boardId) {
        boardRepository.findById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Board %d not found".formatted(boardId)));
        return membershipRepository.findByBoardIdOrderByIdAsc(boardId);
    }

    @Transactional
    @CacheEvict(cacheNames = "board-role", key = "#boardId + ':' + #request.userId()")
    public Membership upsert(Long boardId, CreateMembershipRequest request) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Board %d not found".formatted(boardId)));
        User user = userService.getById(request.userId());

        if (board.getOwner().getId().equals(user.getId()) && request.role() != MembershipRole.ADMIN) {
            throw new IllegalArgumentException("Board owner role must remain ADMIN");
        }

        Membership membership = membershipRepository.findByBoardIdAndUserId(boardId, request.userId())
            .orElseGet(() -> Membership.builder().board(board).user(user).build());

        membership.setRole(request.role());
        return membershipRepository.save(membership);
    }

    @Transactional
    @CacheEvict(cacheNames = "board-role", key = "#boardId + ':' + #userId")
    public void remove(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Board %d not found".formatted(boardId)));

        if (board.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Board owner cannot be removed from membership");
        }

        Membership membership = membershipRepository.findByBoardIdAndUserId(boardId, userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Membership for board %d and user %d not found".formatted(boardId, userId)));

        membershipRepository.delete(membership);
    }

    @Cacheable(cacheNames = "board-role", key = "#boardId + ':' + #userId")
    public Optional<MembershipRole> roleForUser(Long boardId, Long userId) {
        if (boardId == null || userId == null) {
            return Optional.empty();
        }
        return membershipRepository.findByBoardIdAndUserId(boardId, userId)
            .map(Membership::getRole)
            .or(() -> boardRepository.findById(boardId)
                .filter(board -> board.getOwner().getId().equals(userId))
                .map(board -> MembershipRole.ADMIN));
    }
}
