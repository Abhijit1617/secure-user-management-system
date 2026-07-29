package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.comment.CommentResponse;
import com.controlplane.backend.dto.comment.CommentRevisionResponse;
import com.controlplane.backend.dto.comment.CreateCommentRequest;
import com.controlplane.backend.dto.comment.UpdateCommentRequest;
import com.controlplane.backend.entity.Comment;
import com.controlplane.backend.entity.CommentRevision;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.EntityReferenceType;
import com.controlplane.backend.exception.ResourceNotFoundException;
import com.controlplane.backend.mapper.CommentMapper;
import com.controlplane.backend.repository.CommentRepository;
import com.controlplane.backend.repository.CommentRevisionRepository;
import com.controlplane.backend.repository.UserRepository;
import com.controlplane.backend.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9._-]+)");

    private final CommentRepository commentRepository;
    private final CommentRevisionRepository commentRevisionRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> listThread(EntityReferenceType ownerType, UUID ownerId) {
        List<Comment> roots = commentRepository
                .findByOwnerTypeAndOwnerIdAndParentCommentIsNullOrderByCreatedAtAsc(ownerType, ownerId);
        return roots.stream().map(this::buildResponseTree).toList();
    }

    @Override
    @Transactional
    public CommentResponse create(CreateCommentRequest request, UUID authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", "id", authorId));

        Comment parent = null;
        if (request.getParentCommentId() != null) {
            parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Comment", "id", request.getParentCommentId()));
        }

        Comment comment = Comment.builder()
                .ownerType(request.getOwnerType())
                .ownerId(request.getOwnerId())
                .author(author)
                .content(request.getContent())
                .parentComment(parent)
                .mentionedUserIds(extractMentions(request.getContent()))
                .build();

        comment = commentRepository.save(comment);
        log.info("User [{}] commented on {} [{}]", author.getUsername(), request.getOwnerType(),
                request.getOwnerId());
        return commentMapper.toResponse(comment, List.of());
    }

    @Override
    @Transactional
    public CommentResponse update(UUID commentId, UpdateCommentRequest request, UUID requesterId,
                                   boolean requesterIsModerator) {
        Comment comment = loadComment(commentId);
        assertCanModify(comment, requesterId, requesterIsModerator);

        CommentRevision revision = CommentRevision.builder()
                .comment(comment)
                .previousContent(comment.getContent())
                .editedBy(comment.getAuthor())
                .build();
        commentRevisionRepository.save(revision);

        comment.setContent(request.getContent());
        comment.setMentionedUserIds(extractMentions(request.getContent()));
        comment.setEdited(true);
        comment.setEditedAt(Instant.now());

        List<CommentResponse> replies = loadReplies(comment);
        return commentMapper.toResponse(commentRepository.save(comment), replies);
    }

    @Override
    @Transactional
    public void delete(UUID commentId, UUID requesterId, boolean requesterIsModerator) {
        Comment comment = loadComment(commentId);
        assertCanModify(comment, requesterId, requesterIsModerator);

        comment.setDeleted(true);
        comment.setDeletedAt(Instant.now());
        commentRepository.save(comment);
        log.info("Comment [{}] was deleted", commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentRevisionResponse> getEditHistory(UUID commentId) {
        Comment comment = loadComment(commentId);
        return commentRevisionRepository.findByCommentOrderByCreatedAtDesc(comment).stream()
                .map(commentMapper::toRevisionResponse)
                .toList();
    }

    private void assertCanModify(Comment comment, UUID requesterId, boolean requesterIsModerator) {
        if (!requesterIsModerator && !comment.getAuthor().getId().equals(requesterId)) {
            throw new AccessDeniedException("You can only modify your own comments");
        }
    }

    private CommentResponse buildResponseTree(Comment comment) {
        List<CommentResponse> replies = loadReplies(comment);
        return commentMapper.toResponse(comment, replies);
    }

    private List<CommentResponse> loadReplies(Comment comment) {
        return commentRepository.findByParentCommentOrderByCreatedAtAsc(comment).stream()
                .map(this::buildResponseTree)
                .toList();
    }

    private Set<UUID> extractMentions(String content) {
        Set<UUID> mentionedUserIds = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String username = matcher.group(1);
            Optional<User> mentionedUser = userRepository.findByUsername(username);
            mentionedUser.ifPresent(user -> mentionedUserIds.add(user.getId()));
        }
        return mentionedUserIds;
    }

    private Comment loadComment(UUID id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Comment", "id", id));
    }
}
