package com.example.kanban.repository;

import com.example.kanban.domain.Board;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<Board, Long> {

    @Query(
        value = """
            select distinct b
            from Board b
            join Membership m on m.board.id = b.id
            where m.user.id = :userId
              and b.archivedAt is null
              and (:query is null or lower(b.name) like lower(concat('%', :query, '%')))
            """,
        countQuery = """
            select count(distinct b.id)
            from Board b
            join Membership m on m.board.id = b.id
            where m.user.id = :userId
              and b.archivedAt is null
              and (:query is null or lower(b.name) like lower(concat('%', :query, '%')))
            """
    )
    Page<Board> searchAccessibleBoards(@Param("userId") Long userId,
                                       @Param("query") String query,
                                       Pageable pageable);

    Optional<Board> findByIdAndArchivedAtIsNull(Long id);

    @Query("""
        select b.owner.id
        from Board b
        where b.id = :boardId
          and b.archivedAt is null
        """)
    Optional<Long> findActiveOwnerIdById(@Param("boardId") Long boardId);

    Optional<Board> findFirstByName(String name);
}
