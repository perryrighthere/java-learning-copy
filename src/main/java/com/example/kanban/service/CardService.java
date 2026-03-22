package com.example.kanban.service;

import com.example.kanban.domain.BoardColumn;
import com.example.kanban.domain.Card;
import com.example.kanban.realtime.BoardRealtimeNotifier;
import com.example.kanban.repository.BoardColumnRepository;
import com.example.kanban.repository.CardRepository;
import com.example.kanban.web.dto.CreateCardRequest;
import com.example.kanban.web.dto.MoveCardRequest;
import com.example.kanban.web.dto.UpdateCardRequest;
import com.example.kanban.web.error.OptimisticLockConflictException;
import com.example.kanban.web.error.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardService {

    private static final String ORDERING_CONFLICT_CODE = "ordering_conflict";
    private static final List<String> ORDERING_CONFLICT_GUIDANCE = List.of(
        "Reload the target column so the user sees the newest card order.",
        "Ask the user to confirm the destination if another teammate already moved cards.",
        "Retry the move with the latest version and fresh neighbor IDs."
    );

    private final CardRepository cardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final BoardRealtimeNotifier boardRealtimeNotifier;
    private final PositionGapService positionGapService;

    public Page<Card> list(Long columnId, String query, Pageable pageable) {
        requireActiveColumn(columnId);
        return cardRepository.searchActiveByColumnId(columnId, normalizeQuery(query), pageable);
    }

    @Transactional
    public Card create(Long columnId, CreateCardRequest request) {
        BoardColumn column = requireActiveColumn(columnId);
        BigDecimal position = request.position() != null
            ? request.position()
            : positionGapService.nextPosition(cardRepository.findMaxPositionByColumnId(columnId));

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

        assertVersion(card, request.version(), "Card version conflict");

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
            throw staleCardConflict(cardId, "Card was updated by another request");
        }
    }

    @Transactional
    public Card move(Long cardId, MoveCardRequest request) {
        Card card = requireActiveCard(cardId);
        assertVersion(card, request.version(), "Card version conflict");

        BoardColumn targetColumn = requireActiveColumn(request.targetColumnId());
        if (!Objects.equals(card.getColumn().getBoard().getId(), targetColumn.getBoard().getId())) {
            throw new IllegalArgumentException("Cards can only be moved within the same board");
        }

        List<Card> targetCards = new ArrayList<>(cardRepository.findByColumnIdAndDeletedAtIsNullOrderByPositionAscIdAsc(targetColumn.getId()));
        List<Card> originalTargetOrder = card.getColumn().getId().equals(targetColumn.getId())
            ? new ArrayList<>(targetCards)
            : List.of();
        targetCards.removeIf(existing -> existing.getId().equals(cardId));

        PlacementDecision placement = resolvePlacement(card, targetCards, request);
        if (isNoOp(card, targetColumn, originalTargetOrder, placement.index())) {
            return card;
        }

        card.setColumn(targetColumn);

        try {
            if (!placement.requiresRebalance()) {
                card.setPosition(placement.position());
                Card moved = cardRepository.saveAndFlush(card);
                publishMoveEvent(moved);
                return moved;
            }

            List<Card> reorderedCards = new ArrayList<>(targetCards);
            reorderedCards.add(placement.index(), card);
            for (int i = 0; i < reorderedCards.size(); i++) {
                reorderedCards.get(i).setPosition(positionGapService.rebalancePosition(i));
            }

            cardRepository.saveAll(reorderedCards);
            cardRepository.flush();
            publishMoveEvent(card);
            return card;
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw staleCardConflict(cardId, "Card was updated by another request");
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

    private Card requireActiveCard(Long cardId) {
        return cardRepository.findByIdAndDeletedAtIsNull(cardId)
            .orElseThrow(() -> new ResourceNotFoundException("Card %d not found".formatted(cardId)));
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
            "boardId", card.getColumn().getBoard().getId(),
            "columnId", card.getColumn().getId(),
            "title", card.getTitle(),
            "description", card.getDescription() == null ? "" : card.getDescription(),
            "position", card.getPosition(),
            "version", card.getVersion()
        );
    }

    private void assertVersion(Card card, Integer requestedVersion, String message) {
        if (!Objects.equals(requestedVersion, card.getVersion())) {
            throw new OptimisticLockConflictException(message, latestCard(card));
        }
    }

    private OptimisticLockConflictException staleCardConflict(Long cardId, String message) {
        Map<String, Object> latest = cardRepository.findByIdAndDeletedAtIsNull(cardId)
            .map(this::latestCard)
            .orElse(Map.of("id", cardId));
        return new OptimisticLockConflictException(message, latest);
    }

    private OptimisticLockConflictException orderingConflict(Card card, String message) {
        return new OptimisticLockConflictException(
            message,
            latestCard(card),
            ORDERING_CONFLICT_CODE,
            true,
            ORDERING_CONFLICT_GUIDANCE
        );
    }

    private PlacementDecision resolvePlacement(Card card, List<Card> targetCards, MoveCardRequest request) {
        Long previousCardId = request.previousCardId();
        Long nextCardId = request.nextCardId();

        if (Objects.equals(previousCardId, card.getId()) || Objects.equals(nextCardId, card.getId())) {
            throw new IllegalArgumentException("Moved card cannot be used as its own neighbor");
        }
        if (previousCardId != null && previousCardId.equals(nextCardId)) {
            throw new IllegalArgumentException("previousCardId and nextCardId must be different");
        }

        if (targetCards.isEmpty()) {
            if (previousCardId != null || nextCardId != null) {
                throw orderingConflict(card, "Card order changed while you were dragging");
            }
            return new PlacementDecision(0, positionGapService.rebalancePosition(0), false);
        }

        if (previousCardId == null && nextCardId == null) {
            throw new IllegalArgumentException("previousCardId or nextCardId is required when the target column already has cards");
        }

        if (previousCardId != null && nextCardId != null) {
            int previousIndex = indexOfCard(targetCards, previousCardId);
            int nextIndex = indexOfCard(targetCards, nextCardId);
            if (previousIndex < 0 || nextIndex < 0 || previousIndex + 1 != nextIndex) {
                throw orderingConflict(card, "Card order changed while you were dragging");
            }

            return positionBetween(nextIndex, targetCards.get(previousIndex).getPosition(), targetCards.get(nextIndex).getPosition());
        }

        if (previousCardId != null) {
            int previousIndex = indexOfCard(targetCards, previousCardId);
            if (previousIndex < 0 || previousIndex != targetCards.size() - 1) {
                throw orderingConflict(card, "Card order changed while you were dragging");
            }

            BigDecimal nextPosition = positionGapService.nextPosition(targetCards.get(previousIndex).getPosition());
            return new PlacementDecision(targetCards.size(), nextPosition, false);
        }

        int nextIndex = indexOfCard(targetCards, nextCardId);
        if (nextIndex != 0) {
            throw orderingConflict(card, "Card order changed while you were dragging");
        }

        return positionBetween(0, null, targetCards.getFirst().getPosition());
    }

    private PlacementDecision positionBetween(int targetIndex, BigDecimal previousPosition, BigDecimal nextPosition) {
        return positionGapService.positionBetween(previousPosition, nextPosition)
            .map(position -> new PlacementDecision(targetIndex, position, false))
            .orElseGet(() -> new PlacementDecision(targetIndex, null, true));
    }

    private int indexOfCard(List<Card> cards, Long cardId) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getId().equals(cardId)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isNoOp(Card card, BoardColumn targetColumn, List<Card> originalTargetOrder, int targetIndex) {
        if (!Objects.equals(card.getColumn().getId(), targetColumn.getId()) || originalTargetOrder.isEmpty()) {
            return false;
        }
        return indexOfCard(originalTargetOrder, card.getId()) == targetIndex;
    }

    private void publishMoveEvent(Card card) {
        Long resourceVersion = card.getVersion() == null ? Instant.now().toEpochMilli() : card.getVersion().longValue();
        boardRealtimeNotifier.publish("card.moved", card.getColumn().getBoard().getId(), "card", card.getId(), resourceVersion);
    }

    private record PlacementDecision(int index, BigDecimal position, boolean requiresRebalance) {
    }
}
