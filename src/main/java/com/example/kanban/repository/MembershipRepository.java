package com.example.kanban.repository;

import com.example.kanban.domain.Membership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    Optional<Membership> findByBoardIdAndUserId(Long boardId, Long userId);

    @EntityGraph(attributePaths = "user")
    List<Membership> findByBoardIdOrderByIdAsc(Long boardId);
}
