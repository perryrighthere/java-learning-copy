package com.example.kanban.web;

import com.example.kanban.domain.Board;
import com.example.kanban.service.BoardService;
import com.example.kanban.web.dto.BoardResponse;
import com.example.kanban.web.dto.CreateBoardRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Boards", description = "Board endpoints for Week 1 stub")
@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @Operation(summary = "Create a board (owner must exist)")
    @PostMapping
    public ResponseEntity<BoardResponse> create(@Valid @RequestBody CreateBoardRequest request) {
        Board board = boardService.create(request);
        return ResponseEntity.ok(new BoardResponse(board.getId(), board.getName(), board.getOwner().getId()));
    }

    @Operation(summary = "Fetch a board by id")
    @GetMapping("/{id}")
    public ResponseEntity<BoardResponse> get(@PathVariable Long id) {
        Board board = boardService.getById(id);
        return ResponseEntity.ok(new BoardResponse(board.getId(), board.getName(), board.getOwner().getId()));
    }
}
