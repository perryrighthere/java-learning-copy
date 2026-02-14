package com.example.kanban.service;

import com.example.kanban.domain.Board;
import com.example.kanban.domain.Membership;
import com.example.kanban.domain.MembershipRole;
import com.example.kanban.domain.User;
import com.example.kanban.repository.BoardRepository;
import com.example.kanban.repository.MembershipRepository;
import com.example.kanban.web.dto.CreateBoardRequest;
import com.example.kanban.web.error.ResourceNotFoundException;
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

        return saved;
    }

    public Board getById(Long id) {
        return boardRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Board %d not found".formatted(id)));
    }
}
