package com.controlplane.backend.dto.comment;

import com.controlplane.backend.entity.enums.EntityReferenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotNull(message = "Owner type is required")
    private EntityReferenceType ownerType;

    @NotNull(message = "Owner id is required")
    private UUID ownerId;

    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;

    /**
     * Set when this comment is a reply to another comment in the same
     * thread.
     */
    private UUID parentCommentId;
}
