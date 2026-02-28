package com.example.kanban.service;

import com.example.kanban.domain.Card;
import com.example.kanban.domain.CardComment;
import com.example.kanban.domain.User;
import com.example.kanban.realtime.BoardRealtimeNotifier;
import com.example.kanban.repository.CardCommentRepository;
import com.example.kanban.repository.CardRepository;
import com.example.kanban.web.dto.CreateCommentRequest;
import com.example.kanban.web.dto.UpdateCommentRequest;
import com.example.kanban.web.error.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardCommentService {

    private final CardCommentRepository cardCommentRepository;
    private final CardRepository cardRepository;
    private final UserService userService;
    private final BoardRealtimeNotifier boardRealtimeNotifier;

    public Page<CardComment> list(Long cardId, String query, Pageable pageable) {
        requireActiveCard(cardId);
        return cardCommentRepository.searchActiveByCardId(cardId, normalizeQuery(query), pageable);
    }

    @Transactional
    public CardComment create(Long cardId, Long authorId, CreateCommentRequest request) {
        Card card = requireActiveCard(cardId);
        User author = userService.getById(authorId);

        CardComment comment = CardComment.builder()
            .card(card)
            .author(author)
            .body(request.body())
            .build();
        CardComment created = cardCommentRepository.save(comment);
        Long boardId = created.getCard().getColumn().getBoard().getId();
        boardRealtimeNotifier.publish("comment.created", boardId, "comment", created.getId());
        return created;
    }

    public CardComment get(Long cardId, Long commentId) {
        requireActiveCard(cardId);
        return cardCommentRepository.findByIdAndCardIdAndDeletedAtIsNull(commentId, cardId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Comment %d not found on card %d".formatted(commentId, cardId)));
    }

    @Transactional
    public CardComment update(Long cardId, Long commentId, UpdateCommentRequest request) {
        CardComment comment = get(cardId, commentId);
        comment.setBody(request.body());
        CardComment updated = cardCommentRepository.save(comment);
        Long boardId = updated.getCard().getColumn().getBoard().getId();
        boardRealtimeNotifier.publish("comment.updated", boardId, "comment", updated.getId());
        return updated;
    }

    @Transactional
    public void softDelete(Long cardId, Long commentId) {
        CardComment comment = get(cardId, commentId);
        comment.setDeletedAt(Instant.now());
        CardComment deleted = cardCommentRepository.save(comment);
        Long boardId = deleted.getCard().getColumn().getBoard().getId();
        boardRealtimeNotifier.publish("comment.deleted", boardId, "comment", deleted.getId());
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
}
