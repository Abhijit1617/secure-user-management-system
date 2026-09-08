-- ============================================================================
-- V6__add_employee_extensions_attachments_comments.sql
-- Extends employees with organization scoping, address and emergency
-- contact fields, and introduces the shared attachments and comments
-- infrastructure used by the employee, project and task modules.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- employees: organization scoping + address + emergency contact
-- ----------------------------------------------------------------------------
ALTER TABLE employees
    ADD COLUMN organization_id UUID;

ALTER TABLE employees
    ADD CONSTRAINT fk_employees_organization
    FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT;

ALTER TABLE employees
    ALTER COLUMN organization_id SET NOT NULL;

CREATE INDEX idx_employees_organization ON employees (organization_id);

ALTER TABLE employees
    ADD COLUMN address_line1                       VARCHAR(200),
    ADD COLUMN address_line2                       VARCHAR(200),
    ADD COLUMN city                                 VARCHAR(100),
    ADD COLUMN state                                VARCHAR(100),
    ADD COLUMN postal_code                          VARCHAR(20),
    ADD COLUMN country                              VARCHAR(100),
    ADD COLUMN emergency_contact_name                VARCHAR(150),
    ADD COLUMN emergency_contact_relationship        VARCHAR(50),
    ADD COLUMN emergency_contact_phone               VARCHAR(20),
    ADD COLUMN emergency_contact_alternate_phone     VARCHAR(20);

-- ----------------------------------------------------------------------------
-- attachments (employee documents, task attachments, project attachments)
-- ----------------------------------------------------------------------------
CREATE TABLE attachments (
    id                UUID PRIMARY KEY,
    owner_type        VARCHAR(30)  NOT NULL,
    owner_id          UUID         NOT NULL,
    file_name         VARCHAR(255) NOT NULL,
    content_type      VARCHAR(150) NOT NULL,
    file_size_bytes   BIGINT       NOT NULL,
    storage_path      VARCHAR(500) NOT NULL,
    description       VARCHAR(500),
    uploaded_by_id    UUID         NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_attachments_owner ON attachments (owner_type, owner_id);
CREATE INDEX idx_attachments_uploaded_by ON attachments (uploaded_by_id);

-- ----------------------------------------------------------------------------
-- comments (nested, with mentions and soft delete)
-- ----------------------------------------------------------------------------
CREATE TABLE comments (
    id                 UUID PRIMARY KEY,
    owner_type         VARCHAR(30) NOT NULL,
    owner_id           UUID        NOT NULL,
    author_id          UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    content            TEXT        NOT NULL,
    parent_comment_id  UUID REFERENCES comments (id) ON DELETE CASCADE,
    edited             BOOLEAN     NOT NULL DEFAULT FALSE,
    edited_at          TIMESTAMPTZ,
    deleted            BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    version            BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_comments_owner ON comments (owner_type, owner_id);
CREATE INDEX idx_comments_parent ON comments (parent_comment_id);
CREATE INDEX idx_comments_author ON comments (author_id);

CREATE TABLE comment_mentions (
    comment_id       UUID NOT NULL REFERENCES comments (id) ON DELETE CASCADE,
    mentioned_user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (comment_id, mentioned_user_id)
);

CREATE TABLE comment_revisions (
    id                UUID PRIMARY KEY,
    comment_id        UUID        NOT NULL REFERENCES comments (id) ON DELETE CASCADE,
    previous_content  TEXT        NOT NULL,
    edited_by_id      UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_comment_revisions_comment ON comment_revisions (comment_id);
