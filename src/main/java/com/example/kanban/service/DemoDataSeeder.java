package com.example.kanban.service;

import com.example.kanban.domain.Board;
import com.example.kanban.domain.BoardColumn;
import com.example.kanban.domain.Card;
import com.example.kanban.domain.CardComment;
import com.example.kanban.domain.Membership;
import com.example.kanban.domain.MembershipRole;
import com.example.kanban.domain.User;
import com.example.kanban.repository.BoardColumnRepository;
import com.example.kanban.repository.BoardRepository;
import com.example.kanban.repository.CardCommentRepository;
import com.example.kanban.repository.CardRepository;
import com.example.kanban.repository.MembershipRepository;
import com.example.kanban.repository.UserRepository;
import java.math.BigDecimal;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "Password123!";

    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final CardRepository cardRepository;
    private final CardCommentRepository cardCommentRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User alice = seedUser("alice@kanban.local", "Alice Admin", DEFAULT_PASSWORD);
        User bob = seedUser("bob@kanban.local", "Bob Member", DEFAULT_PASSWORD);
        User gina = seedUser("guest@kanban.local", "Gina Guest", DEFAULT_PASSWORD);

        Board board = boardRepository.findFirstByName("Week 2 Demo Board")
            .orElseGet(() -> boardRepository.save(Board.builder()
                .name("Week 2 Demo Board")
                .owner(alice)
                .build()));

        seedMembership(board, alice, MembershipRole.ADMIN);
        seedMembership(board, bob, MembershipRole.MEMBER);
        seedMembership(board, gina, MembershipRole.GUEST);

        BoardColumn todo = seedColumn(board, "To Do", BigDecimal.valueOf(100));
        BoardColumn doing = seedColumn(board, "Doing", BigDecimal.valueOf(200));

        Card firstCard = seedCard(todo, "Week 3 Sample Card", "Demonstrates CRUD, search, and optimistic locking.");
        seedCard(doing, "Week 3 Review", "Card in second column for pagination/search demos.");
        seedCard(doing, "Week 5 Ordering Target", "Second target card so drag/drop demos can place a card between neighbors.");

        seedComment(firstCard, alice, "Remember to include version in card update requests.");
        seedComment(firstCard, bob, "Soft deletes should hide rows from list endpoints.");
    }

    private User seedUser(String email, String displayName, String rawPassword) {
        return userRepository.findByEmail(email)
            .map(existing -> {
                existing.setDisplayName(displayName);
                if (!passwordEncoder.matches(rawPassword, existing.getPasswordHash())) {
                    existing.setPasswordHash(passwordEncoder.encode(rawPassword));
                }
                return userRepository.save(existing);
            })
            .orElseGet(() -> userRepository.save(User.builder()
                .email(email)
                .displayName(displayName)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build()));
    }

    private void seedMembership(Board board, User user, MembershipRole role) {
        Membership membership = membershipRepository.findByBoardIdAndUserId(board.getId(), user.getId())
            .orElseGet(() -> Membership.builder()
                .board(board)
                .user(user)
                .build());

        membership.setRole(role);
        membershipRepository.save(membership);
    }

    private BoardColumn seedColumn(Board board, String name, BigDecimal position) {
        return boardColumnRepository.findFirstByBoardIdAndNameAndDeletedAtIsNull(board.getId(), name)
            .map(existing -> {
                existing.setPosition(position);
                return boardColumnRepository.save(existing);
            })
            .orElseGet(() -> boardColumnRepository.save(BoardColumn.builder()
                .board(board)
                .name(name)
                .position(position)
                .build()));
    }

    private Card seedCard(BoardColumn column, String title, String description) {
        return cardRepository.findFirstByColumnIdAndTitleAndDeletedAtIsNull(column.getId(), title)
            .map(existing -> {
                existing.setDescription(description);
                return cardRepository.save(existing);
            })
            .orElseGet(() -> cardRepository.save(Card.builder()
                .column(column)
                .title(title)
                .description(description)
                .position(nextCardPosition(column.getId()))
                .build()));
    }

    private BigDecimal nextCardPosition(Long columnId) {
        BigDecimal maxPosition = cardRepository.findMaxPositionByColumnId(columnId);
        return maxPosition == null ? BigDecimal.valueOf(100) : maxPosition.add(BigDecimal.valueOf(100));
    }

    private void seedComment(Card card, User author, String body) {
        cardCommentRepository.findFirstByCardIdAndBodyAndDeletedAtIsNull(card.getId(), body)
            .orElseGet(() -> cardCommentRepository.save(CardComment.builder()
                .card(card)
                .author(author)
                .body(body)
                .build()));
    }
}
