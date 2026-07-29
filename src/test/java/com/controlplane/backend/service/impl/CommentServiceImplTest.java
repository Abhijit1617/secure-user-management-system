package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.comment.CreateCommentRequest;
import com.controlplane.backend.dto.comment.UpdateCommentRequest;
import com.controlplane.backend.entity.Comment;
import com.controlplane.backend.entity.CommentRevision;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.EntityReferenceType;
import com.controlplane.backend.mapper.CommentMapper;
import com.controlplane.backend.repository.CommentRepository;
import com.controlplane.backend.repository.CommentRevisionRepository;
import com.controlplane.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentRevisionRepository commentRevisionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User author;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        author = User.builder().username("jane.doe").firstName("Jane").lastName("Doe").build();
        author.setId(authorId);
        lenient().when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createExtractsMentionedUsersFromContent() {
        User mentioned = User.builder().username("bob.smith").build();
        mentioned.setId(UUID.randomUUID());
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(userRepository.findByUsername("bob.smith")).thenReturn(Optional.of(mentioned));

        CreateCommentRequest request = CreateCommentRequest.builder()
                .ownerType(EntityReferenceType.TASK)
                .ownerId(UUID.randomUUID())
                .content("Hey @bob.smith can you take a look?")
                .build();

        commentService.create(request, authorId);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getMentionedUserIds()).containsExactly(mentioned.getId());
    }

    @Test
    void updateByNonAuthorNonModeratorIsRejected() {
        Comment comment = Comment.builder().author(author).content("original").build();
        comment.setId(UUID.randomUUID());
        UUID otherUserId = UUID.randomUUID();
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        UpdateCommentRequest request = UpdateCommentRequest.builder().content("edited").build();

        assertThatThrownBy(() -> commentService.update(comment.getId(), request, otherUserId, false))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void updateByAuthorSavesRevisionAndMarksEdited() {
        Comment comment = Comment.builder().author(author).content("original")
                .mentionedUserIds(java.util.Set.of()).build();
        comment.setId(UUID.randomUUID());
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(commentRepository.findByParentCommentOrderByCreatedAtAsc(comment)).thenReturn(List.of());

        UpdateCommentRequest request = UpdateCommentRequest.builder().content("edited content").build();
        commentService.update(comment.getId(), request, authorId, false);

        ArgumentCaptor<CommentRevision> revisionCaptor = ArgumentCaptor.forClass(CommentRevision.class);
        verify(commentRevisionRepository).save(revisionCaptor.capture());
        assertThat(revisionCaptor.getValue().getPreviousContent()).isEqualTo("original");
        assertThat(comment.isEdited()).isTrue();
        assertThat(comment.getContent()).isEqualTo("edited content");
    }

    @Test
    void deleteByModeratorSucceedsEvenIfNotAuthor() {
        Comment comment = Comment.builder().author(author).content("original").build();
        comment.setId(UUID.randomUUID());
        UUID adminId = UUID.randomUUID();
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        commentService.delete(comment.getId(), adminId, true);

        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getDeletedAt()).isNotNull();
    }
}
