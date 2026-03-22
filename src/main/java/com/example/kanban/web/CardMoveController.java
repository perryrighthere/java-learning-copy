package com.example.kanban.web;

import com.example.kanban.domain.Card;
import com.example.kanban.service.CardService;
import com.example.kanban.web.dto.CardResponse;
import com.example.kanban.web.dto.MoveCardRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Collaboration", description = "Transactional drag/drop ordering and conflict-safe move operations")
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Validated
public class CardMoveController {

    private final CardService cardService;

    @Operation(summary = "Move a card using neighbor-aware ordering with optimistic conflict checks")
    @PatchMapping("/{cardId}/move")
    @PreAuthorize("@boardAccessEvaluator.canWriteCard(#p0, authentication)")
    public ResponseEntity<CardResponse> move(@PathVariable("cardId") Long cardId,
                                             @Valid @RequestBody MoveCardRequest request) {
        Card card = cardService.move(cardId, request);
        return ResponseEntity.ok(toResponse(card));
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
