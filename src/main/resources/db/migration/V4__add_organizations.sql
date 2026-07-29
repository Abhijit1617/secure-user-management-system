-- ============================================================================
-- V4__add_organizations.sql
-- Introduces Organization as the multi-tenancy root. Every department now
-- belongs to exactly one organization, and department codes are scoped to
-- be unique per organization rather than globally.
-- ============================================================================

CREATE TABLE organizations (
    id                                 UUID PRIMARY KEY,
    name                               VARCHAR(150) NOT NULL,
    legal_name                        VARCHAR(200),
    slug                              VARCHAR(100) NOT NULL,
    website                           VARCHAR(255),
    industry                          VARCHAR(100),
    logo_url                          VARCHAR(500),
    status                            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    owner_id                          UUID         NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    settings_timezone                 VARCHAR(50)  NOT NULL DEFAULT 'UTC',
    settings_default_locale           VARCHAR(10)  NOT NULL DEFAULT 'en-US',
    settings_date_format              VARCHAR(20)  NOT NULL DEFAULT 'yyyy-MM-dd',
    settings_currency                 VARCHAR(10)  NOT NULL DEFAULT 'USD',
    settings_allow_self_registration  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by                        VARCHAR(100),
    updated_by                        VARCHAR(100),
    version                           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_organizations_slug UNIQUE (slug)
);

CREATE INDEX idx_organizations_status ON organizations (status);
CREATE INDEX idx_organizations_owner ON organizations (owner_id);

-- ----------------------------------------------------------------------------
-- Scope departments to an organization. The column is added nullable first
-- so this migration is safe to run against a database that already has
-- department rows; it is only tightened to NOT NULL once every row has a
-- value. On a fresh database (the common case for this project so far,
-- since no department has ever been created through the API) this
-- effectively happens instantly.
-- ----------------------------------------------------------------------------
ALTER TABLE departments
    ADD COLUMN organization_id UUID;

ALTER TABLE departments
    ADD CONSTRAINT fk_departments_organization
    FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE;

ALTER TABLE departments
    ALTER COLUMN organization_id SET NOT NULL;

ALTER TABLE departments DROP CONSTRAINT uq_departments_code;
ALTER TABLE departments ADD CONSTRAINT uq_departments_org_code UNIQUE (organization_id, code);

CREATE INDEX idx_departments_organization ON departments (organization_id);
