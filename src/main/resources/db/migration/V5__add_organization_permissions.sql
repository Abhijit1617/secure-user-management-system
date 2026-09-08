-- ============================================================================
-- V5__add_organization_permissions.sql
-- V2 already ran and granted every permission that existed at that time to
-- SUPER_ADMIN/ADMIN via a cross join snapshot; new permissions introduced
-- afterwards must be granted explicitly here, they are not picked up
-- retroactively.
-- ============================================================================

INSERT INTO permissions (id, name, description, module, created_by, updated_by, version) VALUES
    (gen_random_uuid(), 'ORGANIZATION_CREATE', 'Create organizations',            'ORGANIZATION', 'system', 'system', 0),
    (gen_random_uuid(), 'ORGANIZATION_READ',   'View organizations',              'ORGANIZATION', 'system', 'system', 0),
    (gen_random_uuid(), 'ORGANIZATION_UPDATE', 'Update organization details',     'ORGANIZATION', 'system', 'system', 0),
    (gen_random_uuid(), 'ORGANIZATION_DELETE', 'Delete organizations',            'ORGANIZATION', 'system', 'system', 0),
    (gen_random_uuid(), 'ORGANIZATION_MANAGE_OWNERSHIP', 'Transfer organization ownership', 'ORGANIZATION', 'system', 'system', 0);

-- SUPER_ADMIN receives every permission, including any added after V2.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ADMIN receives everything except settings management and ownership transfer,
-- consistent with the exclusion already applied in V2.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name NOT IN ('SETTINGS_MANAGE', 'ORGANIZATION_MANAGE_OWNERSHIP')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- MANAGER and EMPLOYEE can read organization details but not manage them.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name IN ('MANAGER', 'EMPLOYEE', 'VIEWER')
  AND p.name = 'ORGANIZATION_READ'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
