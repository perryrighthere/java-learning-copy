package com.example.kanban.web;

import com.example.kanban.domain.Membership;
import com.example.kanban.service.MembershipService;
import com.example.kanban.web.dto.CreateMembershipRequest;
import com.example.kanban.web.dto.MembershipResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Memberships", description = "Board membership and role management")
@RestController
@RequestMapping("/api/v1/boards/{boardId}/members")
@RequiredArgsConstructor
public class BoardMembershipController {

    private final MembershipService membershipService;

    @Operation(summary = "List board members")
    @GetMapping
    @PreAuthorize("@boardAccessEvaluator.canRead(#p0, authentication)")
    public ResponseEntity<List<MembershipResponse>> list(@PathVariable("boardId") Long boardId) {
        List<MembershipResponse> members = membershipService.listByBoard(boardId)
            .stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(members);
    }

    @Operation(summary = "Add or update board member role")
    @PostMapping
    @PreAuthorize("@boardAccessEvaluator.canManageMembers(#p0, authentication)")
    public ResponseEntity<MembershipResponse> upsert(@PathVariable("boardId") Long boardId,
                                                     @Valid @RequestBody CreateMembershipRequest request) {
        Membership membership = membershipService.upsert(boardId, request);
        return ResponseEntity.ok(toResponse(membership));
    }

    @Operation(summary = "Remove board member")
    @DeleteMapping("/{userId}")
    @PreAuthorize("@boardAccessEvaluator.canManageMembers(#p0, authentication)")
    public ResponseEntity<Void> remove(@PathVariable("boardId") Long boardId,
                                       @PathVariable("userId") Long userId) {
        membershipService.remove(boardId, userId);
        return ResponseEntity.noContent().build();
    }

    private MembershipResponse toResponse(Membership membership) {
        return new MembershipResponse(
            membership.getId(),
            membership.getUser().getId(),
            membership.getUser().getEmail(),
            membership.getUser().getDisplayName(),
            membership.getRole()
        );
    }
}
