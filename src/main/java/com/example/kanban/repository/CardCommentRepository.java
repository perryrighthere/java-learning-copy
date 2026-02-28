package com.example.kanban.repository;

import com.example.kanban.domain.CardComment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardCommentRepository extends JpaRepository<CardComment, Long> {

    @Query("""
        select cc
        from CardComment cc
        where cc.card.id = :cardId
          and cc.deletedAt is null
          and cc.card.deletedAt is null
          and cc.card.column.deletedAt is null
          and cc.card.column.board.archivedAt is null
          and (:query is null or lower(cc.body) like lower(concat('%', :query, '%')))
        """)
    Page<CardComment> searchActiveByCardId(@Param("cardId") Long cardId,
                                           @Param("query") String query,
                                           Pageable pageable);

    Optional<CardComment> findByIdAndDeletedAtIsNull(Long id);

    Optional<CardComment> findByIdAndCardIdAndDeletedAtIsNull(Long id, Long cardId);

    Optional<CardComment> findFirstByCardIdAndBodyAndDeletedAtIsNull(Long cardId, String body);
}
