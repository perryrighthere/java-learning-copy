package com.example.kanban.web;

import com.example.kanban.domain.BoardColumn;
import com.example.kanban.service.BoardColumnService;
import com.example.kanban.web.dto.ColumnResponse;
import com.example.kanban.web.dto.CreateColumnRequest;
import com.example.kanban.web.dto.PagedResponse;
import com.example.kanban.web.dto.UpdateColumnRequest;
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

@Tag(name = "Columns", description = "Board column CRUD with pagination and search")
@RestController
@RequestMapping("/api/v1/boards/{boardId}/columns")
@RequiredArgsConstructor
@Validated
public class BoardColumnController {

    private final BoardColumnService boardColumnService;

    @Operation(summary = "List active columns for a board")
    @GetMapping
    @PreAuthorize("@boardAccessEvaluator.canRead(#p0, authentication)")
    public ResponseEntity<PagedResponse<ColumnResponse>> list(@PathVariable("boardId") Long boardId,
                                                              @RequestParam(name = "q", required = false) String query,
                                                              @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                              @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "position"));
        Page<ColumnResponse> result = boardColumnService.list(boardId, query, pageRequest).map(this::toResponse);
        return ResponseEntity.ok(PagedResponse.from(result));
    }

    @Operation(summary = "Create a column in a board")
    @PostMapping
    @PreAuthorize("@boardAccessEvaluator.canWrite(#p0, authentication)")
    public ResponseEntity<ColumnResponse> create(@PathVariable("boardId") Long boardId,
                                                 @Valid @RequestBody CreateColumnRequest request) {
        BoardColumn column = boardColumnService.create(boardId, request);
        return ResponseEntity.ok(toResponse(column));
    }

    @Operation(summary = "Get a single active column")
    @GetMapping("/{columnId}")
    @PreAuthorize("@boardAccessEvaluator.canRead(#p0, authentication)")
    public ResponseEntity<ColumnResponse> get(@PathVariable("boardId") Long boardId,
                                              @PathVariable("columnId") Long columnId) {
        BoardColumn column = boardColumnService.get(boardId, columnId);
        return ResponseEntity.ok(toResponse(column));
    }

    @Operation(summary = "Update a column")
    @PatchMapping("/{columnId}")
    @PreAuthorize("@boardAccessEvaluator.canWrite(#p0, authentication)")
    public ResponseEntity<ColumnResponse> update(@PathVariable("boardId") Long boardId,
                                                 @PathVariable("columnId") Long columnId,
                                                 @Valid @RequestBody UpdateColumnRequest request) {
        BoardColumn column = boardColumnService.update(boardId, columnId, request);
        return ResponseEntity.ok(toResponse(column));
    }

    @Operation(summary = "Soft-delete a column and all cards under it")
    @DeleteMapping("/{columnId}")
    @PreAuthorize("@boardAccessEvaluator.canWrite(#p0, authentication)")
    public ResponseEntity<Void> delete(@PathVariable("boardId") Long boardId,
                                       @PathVariable("columnId") Long columnId) {
        boardColumnService.softDelete(boardId, columnId);
        return ResponseEntity.noContent().build();
    }

    private ColumnResponse toResponse(BoardColumn column) {
        return new ColumnResponse(
            column.getId(),
            column.getBoard().getId(),
            column.getName(),
            column.getPosition()
        );
    }
}
