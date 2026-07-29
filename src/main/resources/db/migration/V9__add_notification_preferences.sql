-- ============================================================================
-- V9__add_notification_preferences.sql
-- ============================================================================

CREATE TABLE notification_preferences (
    id               UUID PRIMARY KEY,
    user_id          UUID    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    email_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    version          BIGINT  NOT NULL DEFAULT 0,
    CONSTRAINT uq_notification_preferences_user UNIQUE (user_id)
);
