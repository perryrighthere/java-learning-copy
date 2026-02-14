package com.example.kanban.web;

import com.example.kanban.auth.AuthenticatedUser;
import com.example.kanban.domain.Board;
import com.example.kanban.service.BoardService;
import com.example.kanban.web.dto.BoardResponse;
import com.example.kanban.web.dto.CreateBoardRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Boards", description = "Secured board endpoints")
@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @Operation(summary = "Create a board owned by the authenticated user")
    @PostMapping
    public ResponseEntity<BoardResponse> create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                @Valid @RequestBody CreateBoardRequest request) {
        Board board = boardService.create(request, currentUser.id());
        return ResponseEntity.ok(new BoardResponse(board.getId(), board.getName(), board.getOwner().getId()));
    }

    @Operation(summary = "Fetch a board by id")
    @GetMapping("/{id}")
    @PreAuthorize("@boardAccessEvaluator.canRead(#p0, authentication)")
    public ResponseEntity<BoardResponse> get(@PathVariable("id") Long id) {
        Board board = boardService.getById(id);
        return ResponseEntity.ok(new BoardResponse(board.getId(), board.getName(), board.getOwner().getId()));
    }
}
