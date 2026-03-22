package com.example.kanban.repository;

import com.example.kanban.domain.BoardColumn;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, Long> {

    @Query("""
        select c
        from BoardColumn c
        where c.board.id = :boardId
          and c.deletedAt is null
          and c.board.archivedAt is null
          and (:query is null or lower(c.name) like lower(concat('%', :query, '%')))
        """)
    Page<BoardColumn> searchActiveByBoardId(@Param("boardId") Long boardId,
                                            @Param("query") String query,
                                            Pageable pageable);

    Optional<BoardColumn> findByIdAndDeletedAtIsNull(Long id);

    Optional<BoardColumn> findByIdAndBoardIdAndDeletedAtIsNull(Long id, Long boardId);

    Optional<BoardColumn> findFirstByBoardIdAndNameAndDeletedAtIsNull(Long boardId, String name);

    List<BoardColumn> findByBoardIdAndDeletedAtIsNullOrderByPositionAscIdAsc(Long boardId);

    @Query("""
        select c.board.id
        from BoardColumn c
        where c.id = :columnId
          and c.deletedAt is null
          and c.board.archivedAt is null
        """)
    Optional<Long> findActiveBoardIdByColumnId(@Param("columnId") Long columnId);

    @Query("""
        select max(c.position)
        from BoardColumn c
        where c.board.id = :boardId
          and c.deletedAt is null
        """)
    BigDecimal findMaxPositionByBoardId(@Param("boardId") Long boardId);
}
