-- ============================================================================
-- V7__add_comment_and_attachment_permissions.sql
-- ============================================================================

INSERT INTO permissions (id, name, description, module, created_by, updated_by, version) VALUES
    (gen_random_uuid(), 'COMMENT_CREATE',     'Create comments',                'COMMENT',    'system', 'system', 0),
    (gen_random_uuid(), 'COMMENT_READ',       'View comments',                  'COMMENT',    'system', 'system', 0),
    (gen_random_uuid(), 'COMMENT_UPDATE',     'Edit comments',                  'COMMENT',    'system', 'system', 0),
    (gen_random_uuid(), 'COMMENT_DELETE',     'Delete comments',                'COMMENT',    'system', 'system', 0),
    (gen_random_uuid(), 'ATTACHMENT_UPLOAD',  'Upload file attachments',        'ATTACHMENT', 'system', 'system', 0),
    (gen_random_uuid(), 'ATTACHMENT_READ',    'View and download attachments',  'ATTACHMENT', 'system', 'system', 0),
    (gen_random_uuid(), 'ATTACHMENT_DELETE',  'Delete attachments',             'ATTACHMENT', 'system', 'system', 0);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name NOT IN ('SETTINGS_MANAGE', 'ORGANIZATION_MANAGE_OWNERSHIP')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.name IN ('COMMENT_CREATE', 'COMMENT_READ', 'COMMENT_UPDATE', 'COMMENT_DELETE',
                 'ATTACHMENT_UPLOAD', 'ATTACHMENT_READ', 'ATTACHMENT_DELETE')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'EMPLOYEE'
  AND p.name IN ('COMMENT_CREATE', 'COMMENT_READ', 'COMMENT_UPDATE',
                 'ATTACHMENT_UPLOAD', 'ATTACHMENT_READ')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'VIEWER'
  AND p.name IN ('COMMENT_READ', 'ATTACHMENT_READ')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
