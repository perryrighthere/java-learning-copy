package com.example.kanban.service;

import com.example.kanban.domain.Board;
import com.example.kanban.domain.BoardColumn;
import com.example.kanban.realtime.BoardRealtimeNotifier;
import com.example.kanban.repository.BoardColumnRepository;
import com.example.kanban.repository.BoardRepository;
import com.example.kanban.repository.CardRepository;
import com.example.kanban.web.dto.CreateColumnRequest;
import com.example.kanban.web.dto.ReorderColumnsRequest;
import com.example.kanban.web.dto.UpdateColumnRequest;
import com.example.kanban.web.error.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardColumnService {

    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final CardRepository cardRepository;
    private final BoardRealtimeNotifier boardRealtimeNotifier;
    private final PositionGapService positionGapService;

    public Page<BoardColumn> list(Long boardId, String query, Pageable pageable) {
        requireActiveBoard(boardId);
        return boardColumnRepository.searchActiveByBoardId(boardId, normalizeQuery(query), pageable);
    }

    @Transactional
    public BoardColumn create(Long boardId, CreateColumnRequest request) {
        Board board = requireActiveBoard(boardId);
        var position = request.position() != null
            ? request.position()
            : positionGapService.nextPosition(boardColumnRepository.findMaxPositionByBoardId(boardId));

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

    @Transactional
    public List<BoardColumn> reorder(Long boardId, ReorderColumnsRequest request) {
        requireActiveBoard(boardId);

        List<BoardColumn> currentColumns = boardColumnRepository.findByBoardIdAndDeletedAtIsNullOrderByPositionAscIdAsc(boardId);
        validateRequestedOrder(currentColumns, request.orderedColumnIds());

        if (isSameOrder(currentColumns, request.orderedColumnIds())) {
            return currentColumns;
        }

        Map<Long, BoardColumn> columnsById = new HashMap<>();
        currentColumns.forEach(column -> columnsById.put(column.getId(), column));

        List<BoardColumn> reordered = new ArrayList<>(request.orderedColumnIds().size());
        for (int i = 0; i < request.orderedColumnIds().size(); i++) {
            BoardColumn column = columnsById.get(request.orderedColumnIds().get(i));
            column.setPosition(positionGapService.rebalancePosition(i));
            reordered.add(column);
        }

        List<BoardColumn> saved = boardColumnRepository.saveAll(reordered);
        boardRealtimeNotifier.publish("columns.reordered", boardId, "board", boardId);
        return saved;
    }

    private Board requireActiveBoard(Long boardId) {
        return boardRepository.findByIdAndArchivedAtIsNull(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("Board %d not found".formatted(boardId)));
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }

    private void validateRequestedOrder(List<BoardColumn> currentColumns, List<Long> requestedIds) {
        if (currentColumns.size() != requestedIds.size()) {
            throw new IllegalArgumentException("orderedColumnIds must include every active column exactly once");
        }

        Map<Long, Integer> counts = new HashMap<>();
        currentColumns.forEach(column -> counts.put(column.getId(), 0));

        for (Long requestedId : requestedIds) {
            Integer seenCount = counts.get(requestedId);
            if (seenCount == null) {
                throw new IllegalArgumentException("Column %d does not belong to board %d".formatted(requestedId, currentColumns.getFirst().getBoard().getId()));
            }
            if (seenCount > 0) {
                throw new IllegalArgumentException("orderedColumnIds cannot contain duplicates");
            }
            counts.put(requestedId, seenCount + 1);
        }
    }

    private boolean isSameOrder(List<BoardColumn> currentColumns, List<Long> requestedIds) {
        for (int i = 0; i < currentColumns.size(); i++) {
            if (!currentColumns.get(i).getId().equals(requestedIds.get(i))) {
                return false;
            }
        }
        return true;
    }
}
