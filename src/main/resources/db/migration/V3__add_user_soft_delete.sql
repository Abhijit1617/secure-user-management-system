-- ============================================================================
-- V3__add_user_soft_delete.sql
-- Adds soft delete tracking to users so administrators can deactivate and
-- restore accounts without losing employment/audit history tied to them.
-- ============================================================================

ALTER TABLE users
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_users_deleted ON users (deleted);
