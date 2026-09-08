-- ============================================================================
-- V8__add_task_history.sql
-- ============================================================================

CREATE TABLE task_history (
    id             UUID PRIMARY KEY,
    task_id        UUID        NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    field_changed  VARCHAR(50) NOT NULL,
    old_value      VARCHAR(255),
    new_value      VARCHAR(255),
    changed_by_id  UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    version        BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_task_history_task ON task_history (task_id);
