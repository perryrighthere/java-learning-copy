package com.example.kanban.service;

import com.example.kanban.domain.Board;
import com.example.kanban.domain.Membership;
import com.example.kanban.domain.MembershipRole;
import com.example.kanban.domain.User;
import com.example.kanban.repository.BoardRepository;
import com.example.kanban.repository.MembershipRepository;
import com.example.kanban.repository.UserRepository;
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
}
