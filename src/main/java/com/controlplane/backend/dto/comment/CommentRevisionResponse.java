package com.controlplane.backend.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRevisionResponse {

    private UUID id;
    private String previousContent;
    private String editedByUsername;
    private Instant editedAt;
}
