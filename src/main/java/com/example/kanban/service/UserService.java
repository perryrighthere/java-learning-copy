package com.example.kanban.service;

import com.example.kanban.domain.User;
import com.example.kanban.repository.UserRepository;
import com.example.kanban.web.dto.CreateUserRequest;
import com.example.kanban.web.error.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Minimal user service for Week 1 to back REST stubs and validation.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User create(CreateUserRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(existing -> {
            throw new IllegalArgumentException("Email already registered");
        });
        User user = User.builder()
            .email(request.email())
            .displayName(request.displayName())
            .passwordHash(passwordEncoder.encode(request.password()))
            .build();
        return userRepository.save(user);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User %d not found".formatted(id)));
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User with email %s not found".formatted(email)));
    }
}
