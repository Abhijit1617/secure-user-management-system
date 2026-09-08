-- ============================================================================
-- V2__seed_roles_and_permissions.sql
-- Baseline RBAC data required for the platform to be usable out of the box.
-- ============================================================================

INSERT INTO roles (id, name, description, hierarchy_level, created_by, updated_by, version) VALUES
    (gen_random_uuid(), 'SUPER_ADMIN', 'Full unrestricted access to every module', 100, 'system', 'system', 0),
    (gen_random_uuid(), 'ADMIN',       'Administrative access excluding platform level settings', 80, 'system', 'system', 0),
    (gen_random_uuid(), 'MANAGER',     'Manages departments, projects and their assigned employees', 60, 'system', 'system', 0),
    (gen_random_uuid(), 'EMPLOYEE',    'Standard employee access to their own tasks and projects', 40, 'system', 'system', 0),
    (gen_random_uuid(), 'VIEWER',      'Read only access across permitted modules', 20, 'system', 'system', 0);

INSERT INTO permissions (id, name, description, module, created_by, updated_by, version) VALUES
    (gen_random_uuid(), 'USER_CREATE',       'Create user accounts',           'USER',       'system', 'system', 0),
    (gen_random_uuid(), 'USER_READ',         'View user accounts',             'USER',       'system', 'system', 0),
    (gen_random_uuid(), 'USER_UPDATE',       'Update user accounts',           'USER',       'system', 'system', 0),
    (gen_random_uuid(), 'USER_DELETE',       'Delete user accounts',           'USER',       'system', 'system', 0),
    (gen_random_uuid(), 'DEPARTMENT_CREATE', 'Create departments',             'DEPARTMENT', 'system', 'system', 0),
    (gen_random_uuid(), 'DEPARTMENT_READ',   'View departments',               'DEPARTMENT', 'system', 'system', 0),
    (gen_random_uuid(), 'DEPARTMENT_UPDATE', 'Update departments',             'DEPARTMENT', 'system', 'system', 0),
    (gen_random_uuid(), 'DEPARTMENT_DELETE', 'Delete departments',             'DEPARTMENT', 'system', 'system', 0),
    (gen_random_uuid(), 'EMPLOYEE_CREATE',   'Create employee records',        'EMPLOYEE',   'system', 'system', 0),
    (gen_random_uuid(), 'EMPLOYEE_READ',     'View employee records',          'EMPLOYEE',   'system', 'system', 0),
    (gen_random_uuid(), 'EMPLOYEE_UPDATE',   'Update employee records',        'EMPLOYEE',   'system', 'system', 0),
    (gen_random_uuid(), 'EMPLOYEE_DELETE',   'Delete employee records',        'EMPLOYEE',   'system', 'system', 0),
    (gen_random_uuid(), 'PROJECT_CREATE',    'Create projects',                'PROJECT',    'system', 'system', 0),
    (gen_random_uuid(), 'PROJECT_READ',      'View projects',                  'PROJECT',    'system', 'system', 0),
    (gen_random_uuid(), 'PROJECT_UPDATE',    'Update projects',                'PROJECT',    'system', 'system', 0),
    (gen_random_uuid(), 'PROJECT_DELETE',    'Delete projects',                'PROJECT',    'system', 'system', 0),
    (gen_random_uuid(), 'TASK_CREATE',       'Create tasks',                   'TASK',       'system', 'system', 0),
    (gen_random_uuid(), 'TASK_READ',         'View tasks',                     'TASK',       'system', 'system', 0),
    (gen_random_uuid(), 'TASK_UPDATE',       'Update tasks',                   'TASK',       'system', 'system', 0),
    (gen_random_uuid(), 'TASK_DELETE',       'Delete tasks',                   'TASK',       'system', 'system', 0),
    (gen_random_uuid(), 'TASK_ASSIGN',       'Assign tasks to employees',      'TASK',       'system', 'system', 0),
    (gen_random_uuid(), 'AUDIT_READ',        'View audit logs',                'AUDIT',      'system', 'system', 0),
    (gen_random_uuid(), 'SETTINGS_MANAGE',   'Manage platform wide settings',  'SETTINGS',   'system', 'system', 0);

-- SUPER_ADMIN receives every permission that currently exists.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN';

-- ADMIN receives every permission except platform settings management.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN' AND p.name <> 'SETTINGS_MANAGE';

-- MANAGER can manage employees, projects and tasks but not users or settings.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.module IN ('EMPLOYEE', 'PROJECT', 'TASK', 'DEPARTMENT')
  AND p.name NOT LIKE '%_DELETE';

-- EMPLOYEE can read most modules and manage their own tasks.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'EMPLOYEE'
  AND (p.name LIKE '%_READ' OR p.name IN ('TASK_UPDATE', 'TASK_CREATE'));

-- VIEWER is strictly read only.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'VIEWER' AND p.name LIKE '%_READ';
