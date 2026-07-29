package com.controlplane.backend.entity.enums;

/**
 * Used by {@link com.controlplane.backend.entity.Attachment} and
 * {@link com.controlplane.backend.entity.Comment} to record which kind of
 * entity they are attached to, since a single attachments/comments table
 * serves several unrelated parent tables rather than one FK per table.
 */
public enum EntityReferenceType {
    EMPLOYEE,
    PROJECT,
    TASK,
    COMMENT
}
