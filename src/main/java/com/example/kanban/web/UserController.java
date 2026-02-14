package com.example.kanban.web;

import com.example.kanban.auth.AuthenticatedUser;
import com.example.kanban.domain.User;
import com.example.kanban.service.UserService;
import com.example.kanban.web.dto.CreateUserRequest;
import com.example.kanban.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Users", description = "User signup and profile endpoints")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Create a user with a bcrypt-hashed password")
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.create(request);
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getEmail(), user.getDisplayName()));
    }

    @Operation(summary = "Fetch a user by id")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable("id") Long id) {
        User user = userService.getById(id);
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getEmail(), user.getDisplayName()));
    }

    @Operation(summary = "Fetch current authenticated user")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        User user = userService.getById(currentUser.id());
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getEmail(), user.getDisplayName()));
    }
}
