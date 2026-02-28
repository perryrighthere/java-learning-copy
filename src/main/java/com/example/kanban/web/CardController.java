package com.example.kanban.web;

import com.example.kanban.domain.Card;
import com.example.kanban.service.CardService;
import com.example.kanban.web.dto.CardResponse;
import com.example.kanban.web.dto.CreateCardRequest;
import com.example.kanban.web.dto.PagedResponse;
import com.example.kanban.web.dto.UpdateCardRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cards", description = "Card CRUD with optimistic locking, pagination, and search")
@RestController
@RequestMapping("/api/v1/columns/{columnId}/cards")
@RequiredArgsConstructor
@Validated
public class CardController {

    private final CardService cardService;

    @Operation(summary = "List active cards for a column")
    @GetMapping
    @PreAuthorize("@boardAccessEvaluator.canReadColumn(#p0, authentication)")
    public ResponseEntity<PagedResponse<CardResponse>> list(@PathVariable("columnId") Long columnId,
                                                            @RequestParam(name = "q", required = false) String query,
                                                            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "position"));
        Page<CardResponse> result = cardService.list(columnId, query, pageRequest).map(this::toResponse);
        return ResponseEntity.ok(PagedResponse.from(result));
    }

    @Operation(summary = "Create a card in a column")
    @PostMapping
    @PreAuthorize("@boardAccessEvaluator.canWriteColumn(#p0, authentication)")
    public ResponseEntity<CardResponse> create(@PathVariable("columnId") Long columnId,
                                               @Valid @RequestBody CreateCardRequest request) {
        Card card = cardService.create(columnId, request);
        return ResponseEntity.ok(toResponse(card));
    }

    @Operation(summary = "Get a single active card")
    @GetMapping("/{cardId}")
    @PreAuthorize("@boardAccessEvaluator.canReadColumn(#p0, authentication)")
    public ResponseEntity<CardResponse> get(@PathVariable("columnId") Long columnId,
                                            @PathVariable("cardId") Long cardId) {
        Card card = cardService.get(columnId, cardId);
        return ResponseEntity.ok(toResponse(card));
    }

    @Operation(summary = "Update a card using optimistic version checks")
    @PutMapping("/{cardId}")
    @PreAuthorize("@boardAccessEvaluator.canWriteColumn(#p0, authentication)")
    public ResponseEntity<CardResponse> update(@PathVariable("columnId") Long columnId,
                                               @PathVariable("cardId") Long cardId,
                                               @Valid @RequestBody UpdateCardRequest request) {
        Card card = cardService.update(columnId, cardId, request);
        return ResponseEntity.ok(toResponse(card));
    }

    @Operation(summary = "Soft-delete a card")
    @DeleteMapping("/{cardId}")
    @PreAuthorize("@boardAccessEvaluator.canWriteColumn(#p0, authentication)")
    public ResponseEntity<Void> delete(@PathVariable("columnId") Long columnId,
                                       @PathVariable("cardId") Long cardId) {
        cardService.softDelete(columnId, cardId);
        return ResponseEntity.noContent().build();
    }

    private CardResponse toResponse(Card card) {
        return new CardResponse(
            card.getId(),
            card.getColumn().getId(),
            card.getTitle(),
            card.getDescription(),
            card.getPosition(),
            card.getVersion(),
            card.getDeletedAt()
        );
    }
}
