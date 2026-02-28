package com.example.kanban.service;

import com.example.kanban.domain.Board;
import com.example.kanban.domain.Membership;
import com.example.kanban.domain.MembershipRole;
import com.example.kanban.domain.User;
import com.example.kanban.realtime.BoardRealtimeNotifier;
import com.example.kanban.repository.BoardRepository;
import com.example.kanban.repository.MembershipRepository;
import com.example.kanban.web.dto.UpdateBoardRequest;
import com.example.kanban.web.dto.CreateBoardRequest;
import com.example.kanban.web.error.ResourceNotFoundException;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Minimal board service used for OpenAPI-ready endpoints.
 */
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final MembershipRepository membershipRepository;
    private final UserService userService;
    private final BoardRealtimeNotifier boardRealtimeNotifier;

    public Page<Board> listAccessible(Long userId, String query, Pageable pageable) {
        return boardRepository.searchAccessibleBoards(userId, normalizeQuery(query), pageable);
    }

    @Transactional
    public Board create(CreateBoardRequest request, Long ownerId) {
        User owner = userService.getById(ownerId);
        Board board = Board.builder()
            .name(request.name())
            .owner(owner)
            .build();
        Board saved = boardRepository.save(board);

        membershipRepository.findByBoardIdAndUserId(saved.getId(), ownerId)
            .orElseGet(() -> membershipRepository.save(
                Membership.builder()
                    .board(saved)
                    .user(owner)
                    .role(MembershipRole.ADMIN)
                    .build()
            ));

        boardRealtimeNotifier.publish("board.created", saved.getId(), "board", saved.getId());
        return saved;
    }

    @Transactional
    public Board update(Long id, UpdateBoardRequest request) {
        Board board = getById(id);
        board.setName(request.name());
        Board updated = boardRepository.save(board);
        boardRealtimeNotifier.publish("board.updated", updated.getId(), "board", updated.getId());
        return updated;
    }

    @Transactional
    public void archive(Long id) {
        Board board = getById(id);
        board.setArchivedAt(Instant.now());
        Board archived = boardRepository.save(board);
        boardRealtimeNotifier.publish("board.archived", archived.getId(), "board", archived.getId());
    }

    public Board getById(Long id) {
        return boardRepository.findByIdAndArchivedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Board %d not found".formatted(id)));
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }
}
