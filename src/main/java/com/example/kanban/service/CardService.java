package com.example.kanban.service;

import com.example.kanban.domain.BoardColumn;
import com.example.kanban.domain.Card;
import com.example.kanban.realtime.BoardRealtimeNotifier;
import com.example.kanban.repository.BoardColumnRepository;
import com.example.kanban.repository.CardRepository;
import com.example.kanban.web.dto.CreateCardRequest;
import com.example.kanban.web.dto.UpdateCardRequest;
import com.example.kanban.web.error.OptimisticLockConflictException;
import com.example.kanban.web.error.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardService {

    private static final BigDecimal POSITION_GAP = BigDecimal.valueOf(100);

    private final CardRepository cardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final BoardRealtimeNotifier boardRealtimeNotifier;

    public Page<Card> list(Long columnId, String query, Pageable pageable) {
        requireActiveColumn(columnId);
        return cardRepository.searchActiveByColumnId(columnId, normalizeQuery(query), pageable);
    }

    @Transactional
    public Card create(Long columnId, CreateCardRequest request) {
        BoardColumn column = requireActiveColumn(columnId);
        BigDecimal position = request.position() != null
            ? request.position()
            : nextPosition(cardRepository.findMaxPositionByColumnId(columnId));

        Card card = Card.builder()
            .column(column)
            .title(request.title())
            .description(request.description())
            .position(position)
            .build();
        Card created = cardRepository.save(card);
        Long boardId = created.getColumn().getBoard().getId();
        boardRealtimeNotifier.publish("card.created", boardId, "card", created.getId());
        return created;
    }

    public Card get(Long columnId, Long cardId) {
        requireActiveColumn(columnId);
        return cardRepository.findByIdAndColumnIdAndDeletedAtIsNull(cardId, columnId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Card %d not found in column %d".formatted(cardId, columnId)));
    }

    @Transactional
    public Card update(Long columnId, Long cardId, UpdateCardRequest request) {
        Card card = get(columnId, cardId);

        if (!request.version().equals(card.getVersion())) {
            throw new OptimisticLockConflictException("Card version conflict", latestCard(card));
        }

        card.setTitle(request.title());
        card.setDescription(request.description());
        if (request.position() != null) {
            card.setPosition(request.position());
        }

        try {
            Card updated = cardRepository.saveAndFlush(card);
            Long boardId = updated.getColumn().getBoard().getId();
            boardRealtimeNotifier.publish(
                "card.updated",
                boardId,
                "card",
                updated.getId(),
                updated.getVersion() == null ? Instant.now().toEpochMilli() : updated.getVersion().longValue()
            );
            return updated;
        } catch (ObjectOptimisticLockingFailureException ex) {
            Map<String, Object> latest = cardRepository.findByIdAndDeletedAtIsNull(cardId)
                .map(this::latestCard)
                .orElse(Map.of("id", cardId));
            throw new OptimisticLockConflictException("Card was updated by another request", latest);
        }
    }

    @Transactional
    public void softDelete(Long columnId, Long cardId) {
        Card card = get(columnId, cardId);
        card.setDeletedAt(Instant.now());
        Card deleted = cardRepository.save(card);
        Long boardId = deleted.getColumn().getBoard().getId();
        boardRealtimeNotifier.publish("card.deleted", boardId, "card", deleted.getId());
    }

    private BoardColumn requireActiveColumn(Long columnId) {
        return boardColumnRepository.findByIdAndDeletedAtIsNull(columnId)
            .orElseThrow(() -> new ResourceNotFoundException("Column %d not found".formatted(columnId)));
    }

    private BigDecimal nextPosition(BigDecimal maxPosition) {
        return maxPosition == null ? POSITION_GAP : maxPosition.add(POSITION_GAP);
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }

    private Map<String, Object> latestCard(Card card) {
        return Map.of(
            "id", card.getId(),
            "columnId", card.getColumn().getId(),
            "title", card.getTitle(),
            "description", card.getDescription() == null ? "" : card.getDescription(),
            "position", card.getPosition(),
            "version", card.getVersion()
        );
    }
}
