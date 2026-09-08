-- ============================================================================
-- V1__init_schema.sql
-- Backend Control Plane - baseline schema
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ----------------------------------------------------------------------------
-- roles
-- ----------------------------------------------------------------------------
CREATE TABLE roles (
    id               UUID PRIMARY KEY,
    name             VARCHAR(50)  NOT NULL,
    description      VARCHAR(255),
    hierarchy_level  INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_roles_name UNIQUE (name)
);

-- ----------------------------------------------------------------------------
-- permissions
-- ----------------------------------------------------------------------------
CREATE TABLE permissions (
    id               UUID PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    description      VARCHAR(255),
    module           VARCHAR(50)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_permissions_name UNIQUE (name)
);

CREATE INDEX idx_permissions_module ON permissions (module);

-- ----------------------------------------------------------------------------
-- role_permissions (join table)
-- ----------------------------------------------------------------------------
CREATE TABLE role_permissions (
    role_id        UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id  UUID NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- ----------------------------------------------------------------------------
-- users
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id                     UUID PRIMARY KEY,
    username               VARCHAR(50)  NOT NULL,
    email                  VARCHAR(150) NOT NULL,
    password               VARCHAR(255) NOT NULL,
    first_name             VARCHAR(80)  NOT NULL,
    last_name              VARCHAR(80)  NOT NULL,
    phone_number           VARCHAR(20),
    status                 VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified         BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts  INTEGER      NOT NULL DEFAULT 0,
    account_locked_until   TIMESTAMPTZ,
    last_login_at          TIMESTAMPTZ,
    password_changed_at    TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100),
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_status ON users (status);

-- ----------------------------------------------------------------------------
-- user_roles (join table)
-- ----------------------------------------------------------------------------
CREATE TABLE user_roles (
    user_id  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id  UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ----------------------------------------------------------------------------
-- departments (self referential hierarchy)
-- ----------------------------------------------------------------------------
CREATE TABLE departments (
    id                    UUID PRIMARY KEY,
    name                  VARCHAR(150) NOT NULL,
    code                  VARCHAR(20)  NOT NULL,
    description           VARCHAR(500),
    parent_department_id  UUID REFERENCES departments (id) ON DELETE SET NULL,
    head_employee_id      UUID,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    version               BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_departments_code UNIQUE (code)
);

CREATE INDEX idx_departments_parent ON departments (parent_department_id);

-- ----------------------------------------------------------------------------
-- employees
-- ----------------------------------------------------------------------------
CREATE TABLE employees (
    id                     UUID PRIMARY KEY,
    employee_code          VARCHAR(30)  NOT NULL,
    user_id                UUID         NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    department_id          UUID REFERENCES departments (id) ON DELETE SET NULL,
    designation            VARCHAR(100),
    employment_type        VARCHAR(30)  NOT NULL DEFAULT 'FULL_TIME',
    status                 VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    date_of_joining        DATE         NOT NULL,
    date_of_exit           DATE,
    reporting_manager_id   UUID REFERENCES employees (id) ON DELETE SET NULL,
    annual_ctc             NUMERIC(14,2),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100),
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_employees_code UNIQUE (employee_code),
    CONSTRAINT uq_employees_user UNIQUE (user_id)
);

CREATE INDEX idx_employees_department ON employees (department_id);
CREATE INDEX idx_employees_status ON employees (status);

ALTER TABLE departments
    ADD CONSTRAINT fk_departments_head_employee
    FOREIGN KEY (head_employee_id) REFERENCES employees (id) ON DELETE SET NULL;

-- ----------------------------------------------------------------------------
-- projects
-- ----------------------------------------------------------------------------
CREATE TABLE projects (
    id                  UUID PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    code                VARCHAR(30)  NOT NULL,
    description         VARCHAR(2000),
    status              VARCHAR(30)  NOT NULL DEFAULT 'PLANNED',
    priority            VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    start_date          DATE,
    end_date            DATE,
    department_id       UUID REFERENCES departments (id) ON DELETE SET NULL,
    project_manager_id  UUID REFERENCES employees (id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_projects_code UNIQUE (code)
);

CREATE INDEX idx_projects_status ON projects (status);
CREATE INDEX idx_projects_department ON projects (department_id);

-- ----------------------------------------------------------------------------
-- project_members (join table)
-- ----------------------------------------------------------------------------
CREATE TABLE project_members (
    project_id   UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    employee_id  UUID NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    PRIMARY KEY (project_id, employee_id)
);

-- ----------------------------------------------------------------------------
-- tasks
-- ----------------------------------------------------------------------------
CREATE TABLE tasks (
    id               UUID PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    description      VARCHAR(3000),
    status           VARCHAR(30)  NOT NULL DEFAULT 'TODO',
    priority         VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    project_id       UUID         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    assignee_id      UUID REFERENCES employees (id) ON DELETE SET NULL,
    reporter_id      UUID REFERENCES employees (id) ON DELETE SET NULL,
    due_date         DATE,
    completed_at     TIMESTAMPTZ,
    estimated_hours  DOUBLE PRECISION,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    version          BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_tasks_project ON tasks (project_id);
CREATE INDEX idx_tasks_assignee ON tasks (assignee_id);
CREATE INDEX idx_tasks_status ON tasks (status);
CREATE INDEX idx_tasks_due_date ON tasks (due_date);

-- ----------------------------------------------------------------------------
-- notifications
-- ----------------------------------------------------------------------------
CREATE TABLE notifications (
    id              UUID PRIMARY KEY,
    recipient_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title           VARCHAR(150) NOT NULL,
    message         VARCHAR(1000) NOT NULL,
    type            VARCHAR(40)  NOT NULL,
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,
    reference_type  VARCHAR(50),
    reference_id    UUID,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_id);
CREATE INDEX idx_notifications_recipient_read ON notifications (recipient_id, is_read);

-- ----------------------------------------------------------------------------
-- audit_logs (append only)
-- ----------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id           UUID PRIMARY KEY,
    user_id      UUID REFERENCES users (id) ON DELETE SET NULL,
    action       VARCHAR(30)  NOT NULL,
    entity_name  VARCHAR(100),
    entity_id    UUID,
    old_value    TEXT,
    new_value    TEXT,
    ip_address   VARCHAR(64),
    user_agent   VARCHAR(255),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    version      BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_audit_logs_user ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_name, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);

-- ----------------------------------------------------------------------------
-- refresh_tokens
-- ----------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token               VARCHAR(512) NOT NULL,
    expiry_date         TIMESTAMPTZ  NOT NULL,
    revoked             BOOLEAN      NOT NULL DEFAULT FALSE,
    replaced_by_token   VARCHAR(512),
    device_info         VARCHAR(255),
    ip_address          VARCHAR(64),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_refresh_tokens_token UNIQUE (token)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
