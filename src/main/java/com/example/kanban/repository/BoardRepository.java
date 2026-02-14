package com.example.kanban.repository;

import com.example.kanban.domain.Board;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long> {
    Optional<Board> findFirstByName(String name);
}
