package com.example.kanban.service;

import com.example.kanban.domain.Board;
import com.example.kanban.domain.BoardColumn;
import com.example.kanban.realtime.BoardRealtimeNotifier;
import com.example.kanban.repository.BoardColumnRepository;
import com.example.kanban.repository.BoardRepository;
import com.example.kanban.repository.CardRepository;
import com.example.kanban.web.dto.CreateColumnRequest;
import com.example.kanban.web.dto.UpdateColumnRequest;
import com.example.kanban.web.error.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardColumnService {

    private static final BigDecimal POSITION_GAP = BigDecimal.valueOf(100);

    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final CardRepository cardRepository;
    private final BoardRealtimeNotifier boardRealtimeNotifier;

    public Page<BoardColumn> list(Long boardId, String query, Pageable pageable) {
        requireActiveBoard(boardId);
        return boardColumnRepository.searchActiveByBoardId(boardId, normalizeQuery(query), pageable);
    }

    @Transactional
    public BoardColumn create(Long boardId, CreateColumnRequest request) {
        Board board = requireActiveBoard(boardId);
        BigDecimal position = request.position() != null
            ? request.position()
            : nextPosition(boardColumnRepository.findMaxPositionByBoardId(boardId));

        BoardColumn column = BoardColumn.builder()
            .board(board)
            .name(request.name())
            .position(position)
            .build();
        BoardColumn created = boardColumnRepository.save(column);
        boardRealtimeNotifier.publish("column.created", boardId, "column", created.getId());
        return created;
    }

    public BoardColumn get(Long boardId, Long columnId) {
        requireActiveBoard(boardId);
        return boardColumnRepository.findByIdAndBoardIdAndDeletedAtIsNull(columnId, boardId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Column %d not found in board %d".formatted(columnId, boardId)));
    }

    @Transactional
    public BoardColumn update(Long boardId, Long columnId, UpdateColumnRequest request) {
        BoardColumn column = get(boardId, columnId);
        column.setName(request.name());
        if (request.position() != null) {
            column.setPosition(request.position());
        }
        BoardColumn updated = boardColumnRepository.save(column);
        boardRealtimeNotifier.publish("column.updated", boardId, "column", updated.getId());
        return updated;
    }

    @Transactional
    public void softDelete(Long boardId, Long columnId) {
        BoardColumn column = get(boardId, columnId);
        Instant deletedAt = Instant.now();
        column.setDeletedAt(deletedAt);
        BoardColumn deleted = boardColumnRepository.save(column);
        cardRepository.softDeleteByColumnId(column.getId(), deletedAt);
        boardRealtimeNotifier.publish("column.deleted", boardId, "column", deleted.getId());
    }

    private Board requireActiveBoard(Long boardId) {
        return boardRepository.findByIdAndArchivedAtIsNull(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Board %d not found".formatted(boardId)));
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
}
