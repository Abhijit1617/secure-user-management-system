package com.controlplane.backend.controller;

import com.controlplane.backend.dto.comment.CommentResponse;
import com.controlplane.backend.dto.comment.CommentRevisionResponse;
import com.controlplane.backend.dto.comment.CreateCommentRequest;
import com.controlplane.backend.dto.comment.UpdateCommentRequest;
import com.controlplane.backend.entity.enums.EntityReferenceType;
import com.controlplane.backend.security.CustomUserDetails;
import com.controlplane.backend.security.SecurityConstants;
import com.controlplane.backend.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Comment Management", description = "Nested comments with @mentions, edit history and soft delete")
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    @PreAuthorize("hasAuthority('COMMENT_READ')")
    @Operation(summary = "Get the full comment thread for an owning entity (task or project), nested as a tree")
    public ResponseEntity<List<CommentResponse>> listThread(@RequestParam EntityReferenceType ownerType,
                                                             @RequestParam UUID ownerId) {
        return ResponseEntity.ok(commentService.listThread(ownerType, ownerId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COMMENT_CREATE')")
    @Operation(summary = "Post a comment, optionally as a reply to another comment",
            description = "Any @username token in the content is resolved and recorded as a mention.")
    public ResponseEntity<CommentResponse> create(@AuthenticationPrincipal CustomUserDetails principal,
                                                   @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.create(request, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COMMENT_UPDATE')")
    @Operation(summary = "Edit a comment",
            description = "Only the original author may edit their own comment. The previous content is "
                    + "preserved and retrievable via the edit-history endpoint.")
    public ResponseEntity<CommentResponse> update(@AuthenticationPrincipal CustomUserDetails principal,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody UpdateCommentRequest request) {
        boolean isModerator = isModerator(principal);
        return ResponseEntity.ok(commentService.update(id, request, principal.getId(), isModerator));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('COMMENT_DELETE')")
    @Operation(summary = "Soft delete a comment",
            description = "The comment is replaced with a tombstone; replies remain visible.")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID id) {
        boolean isModerator = isModerator(principal);
        commentService.delete(id, principal.getId(), isModerator);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('COMMENT_READ')")
    @Operation(summary = "View the edit history of a comment")
    public ResponseEntity<List<CommentRevisionResponse>> getEditHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(commentService.getEditHistory(id));
    }

    private boolean isModerator(CustomUserDetails principal) {
        return principal.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(SecurityConstants.ROLE_PREFIX + "ADMIN")
                        || authority.getAuthority().equals(SecurityConstants.ROLE_PREFIX + "SUPER_ADMIN"));
    }
}
