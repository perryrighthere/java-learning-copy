package com.example.kanban.web;

import com.example.kanban.auth.AuthenticatedUser;
import com.example.kanban.domain.CardComment;
import com.example.kanban.service.CardCommentService;
import com.example.kanban.web.dto.CommentResponse;
import com.example.kanban.web.dto.CreateCommentRequest;
import com.example.kanban.web.dto.PagedResponse;
import com.example.kanban.web.dto.UpdateCommentRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Comments", description = "Comment CRUD with pagination and search")
@RestController
@RequestMapping("/api/v1/cards/{cardId}/comments")
@RequiredArgsConstructor
@Validated
public class CardCommentController {

    private final CardCommentService cardCommentService;

    @Operation(summary = "List active comments for a card")
    @GetMapping
    @PreAuthorize("@boardAccessEvaluator.canReadCard(#p0, authentication)")
    public ResponseEntity<PagedResponse<CommentResponse>> list(@PathVariable("cardId") Long cardId,
                                                               @RequestParam(name = "q", required = false) String query,
                                                               @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                               @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CommentResponse> result = cardCommentService.list(cardId, query, pageRequest).map(this::toResponse);
        return ResponseEntity.ok(PagedResponse.from(result));
    }

    @Operation(summary = "Create a comment on a card")
    @PostMapping
    @PreAuthorize("@boardAccessEvaluator.canWriteCard(#p0, authentication)")
    public ResponseEntity<CommentResponse> create(@PathVariable("cardId") Long cardId,
                                                  @AuthenticationPrincipal AuthenticatedUser currentUser,
                                                  @Valid @RequestBody CreateCommentRequest request) {
        CardComment comment = cardCommentService.create(cardId, currentUser.id(), request);
        return ResponseEntity.ok(toResponse(comment));
    }

    @Operation(summary = "Get a single active comment")
    @GetMapping("/{commentId}")
    @PreAuthorize("@boardAccessEvaluator.canReadCard(#p0, authentication)")
    public ResponseEntity<CommentResponse> get(@PathVariable("cardId") Long cardId,
                                               @PathVariable("commentId") Long commentId) {
        CardComment comment = cardCommentService.get(cardId, commentId);
        return ResponseEntity.ok(toResponse(comment));
    }

    @Operation(summary = "Update a comment")
    @PutMapping("/{commentId}")
    @PreAuthorize("@boardAccessEvaluator.canWriteCard(#p0, authentication)")
    public ResponseEntity<CommentResponse> update(@PathVariable("cardId") Long cardId,
                                                  @PathVariable("commentId") Long commentId,
                                                  @Valid @RequestBody UpdateCommentRequest request) {
        CardComment comment = cardCommentService.update(cardId, commentId, request);
        return ResponseEntity.ok(toResponse(comment));
    }

    @Operation(summary = "Soft-delete a comment")
    @DeleteMapping("/{commentId}")
    @PreAuthorize("@boardAccessEvaluator.canWriteCard(#p0, authentication)")
    public ResponseEntity<Void> delete(@PathVariable("cardId") Long cardId,
                                       @PathVariable("commentId") Long commentId) {
        cardCommentService.softDelete(cardId, commentId);
        return ResponseEntity.noContent().build();
    }

    private CommentResponse toResponse(CardComment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getCard().getId(),
            comment.getAuthor().getId(),
            comment.getAuthor().getDisplayName(),
            comment.getBody(),
            comment.getCreatedAt(),
            comment.getUpdatedAt()
        );
    }
}
