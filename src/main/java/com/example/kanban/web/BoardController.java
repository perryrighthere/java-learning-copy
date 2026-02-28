package com.example.kanban.web;

import com.example.kanban.auth.AuthenticatedUser;
import com.example.kanban.domain.Board;
import com.example.kanban.service.BoardService;
import com.example.kanban.web.dto.BoardResponse;
import com.example.kanban.web.dto.CreateBoardRequest;
import com.example.kanban.web.dto.PagedResponse;
import com.example.kanban.web.dto.UpdateBoardRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Boards", description = "Secured board endpoints")
@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
@Validated
public class BoardController {

    private final BoardService boardService;

    @Operation(summary = "List accessible boards with pagination and optional search")
    @GetMapping
    public ResponseEntity<PagedResponse<BoardResponse>> list(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                             @RequestParam(name = "q", required = false) String query,
                                                             @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                             @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<BoardResponse> result = boardService.listAccessible(currentUser.id(), query, pageRequest).map(this::toResponse);
        return ResponseEntity.ok(PagedResponse.from(result));
    }

    @Operation(summary = "Create a board owned by the authenticated user")
    @PostMapping
    public ResponseEntity<BoardResponse> create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                @Valid @RequestBody CreateBoardRequest request) {
        Board board = boardService.create(request, currentUser.id());
        return ResponseEntity.ok(toResponse(board));
    }

    @Operation(summary = "Fetch an active board by id")
    @GetMapping("/{id}")
    @PreAuthorize("@boardAccessEvaluator.canRead(#p0, authentication)")
    public ResponseEntity<BoardResponse> get(@PathVariable("id") Long id) {
        Board board = boardService.getById(id);
        return ResponseEntity.ok(toResponse(board));
    }

    @Operation(summary = "Update board metadata")
    @PatchMapping("/{id}")
    @PreAuthorize("@boardAccessEvaluator.canManageBoard(#p0, authentication)")
    public ResponseEntity<BoardResponse> update(@PathVariable("id") Long id,
                                                @Valid @RequestBody UpdateBoardRequest request) {
        Board board = boardService.update(id, request);
        return ResponseEntity.ok(toResponse(board));
    }

    @Operation(summary = "Soft-delete a board by setting archivedAt")
    @DeleteMapping("/{id}")
    @PreAuthorize("@boardAccessEvaluator.canManageBoard(#p0, authentication)")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        boardService.archive(id);
        return ResponseEntity.noContent().build();
    }

    private BoardResponse toResponse(Board board) {
        return new BoardResponse(board.getId(), board.getName(), board.getOwner().getId());
    }
}
