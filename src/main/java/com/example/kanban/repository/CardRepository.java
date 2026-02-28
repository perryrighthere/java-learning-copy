package com.example.kanban.repository;

import com.example.kanban.domain.Card;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long> {

    @Query("""
        select c
        from Card c
        where c.column.id = :columnId
          and c.deletedAt is null
          and c.column.deletedAt is null
          and c.column.board.archivedAt is null
          and (
            :query is null
            or lower(c.title) like lower(concat('%', :query, '%'))
            or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%'))
          )
        """)
    Page<Card> searchActiveByColumnId(@Param("columnId") Long columnId,
                                      @Param("query") String query,
                                      Pageable pageable);

    Optional<Card> findByIdAndDeletedAtIsNull(Long id);

    Optional<Card> findByIdAndColumnIdAndDeletedAtIsNull(Long id, Long columnId);

    Optional<Card> findFirstByColumnIdAndTitleAndDeletedAtIsNull(Long columnId, String title);

    @Query("""
        select c.column.board.id
        from Card c
        where c.id = :cardId
          and c.deletedAt is null
          and c.column.deletedAt is null
          and c.column.board.archivedAt is null
        """)
    Optional<Long> findActiveBoardIdByCardId(@Param("cardId") Long cardId);

    @Query("""
        select max(c.position)
        from Card c
        where c.column.id = :columnId
          and c.deletedAt is null
        """)
    BigDecimal findMaxPositionByColumnId(@Param("columnId") Long columnId);

    @Modifying
    @Query("""
        update Card c
        set c.deletedAt = :deletedAt
        where c.column.id = :columnId
          and c.deletedAt is null
        """)
    int softDeleteByColumnId(@Param("columnId") Long columnId, @Param("deletedAt") Instant deletedAt);
}
