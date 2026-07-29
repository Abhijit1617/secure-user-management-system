# API Guide

Full request/response schemas are in Swagger UI at `/swagger-ui.html`
once the app is running; worked `curl` examples for the authentication
flow are in [`api-examples.md`](api-examples.md). This document is the
endpoint index — every route, grouped by module, with the permission it
requires.

All routes are prefixed `/api/v1`. "Auth" means any authenticated user;
a specific permission name means `@PreAuthorize("hasAuthority('X')")`;
`SUPER_ADMIN`/`ADMIN` means a role check.

## Authentication (`/auth`)
| Method | Path | Auth |
|---|---|---|
| POST | `/auth/register` | none |
| POST | `/auth/login` | none |
| POST | `/auth/refresh` | none |
| POST | `/auth/logout` | none |
| GET | `/auth/me` | any user |
| POST | `/auth/change-password` | any user |
| POST | `/auth/forgot-password` | none |
| POST | `/auth/reset-password` | none |
| GET | `/auth/verify-email` | none |
| POST | `/auth/resend-verification` | none |
| DELETE | `/auth/deactivate` | any user |
| PUT | `/auth/reactivate/{userId}` | `USER_UPDATE` |

## Users (`/users`)
| Method | Path | Auth |
|---|---|---|
| GET | `/users` | `USER_READ` |
| GET | `/users/{id}` | `USER_READ` |
| POST | `/users` | `USER_CREATE` |
| PUT | `/users/{id}` | `USER_UPDATE` |
| PUT | `/users/me` | any user |
| DELETE | `/users/{id}` | `USER_DELETE` |
| PUT | `/users/{id}/restore` \| `/enable` \| `/disable` | `USER_UPDATE` |
| POST/DELETE | `/users/{id}/roles[/{roleId}]` | `USER_UPDATE` |

## Roles & Permissions (`/roles`, `/permissions`)
CRUD plus `/roles/{id}/permissions[/{permissionId}]` for assignment.
Read requires `USER_READ`; write requires `SUPER_ADMIN`.

## Organizations (`/organizations`)
| Method | Path | Auth |
|---|---|---|
| GET | `/organizations` \| `/{id}` | `ORGANIZATION_READ` |
| POST | `/organizations` | `ORGANIZATION_CREATE` |
| PUT | `/organizations/{id}` \| `/logo` \| `/status` | `ORGANIZATION_UPDATE` |
| PUT | `/organizations/{id}/ownership` | `ORGANIZATION_MANAGE_OWNERSHIP` |
| DELETE | `/organizations/{id}` | `ORGANIZATION_DELETE` |

## Departments (`/departments`)
| Method | Path | Auth |
|---|---|---|
| GET | `/departments` \| `/{id}` \| `/hierarchy` \| `/{id}/statistics` | `DEPARTMENT_READ` |
| POST | `/departments` | `DEPARTMENT_CREATE` |
| PUT | `/departments/{id}` \| `/manager` \| `/activate` \| `/deactivate` | `DEPARTMENT_UPDATE` |
| DELETE | `/departments/{id}` \| `/{id}/manager` | `DEPARTMENT_DELETE` / `DEPARTMENT_UPDATE` |

## Employees (`/employees`)
| Method | Path | Auth |
|---|---|---|
| GET | `/employees` \| `/{id}` | `EMPLOYEE_READ` |
| POST | `/employees` | `EMPLOYEE_CREATE` |
| PUT | `/employees/{id}` \| `/profile` \| `/status` | `EMPLOYEE_UPDATE` |
| DELETE | `/employees/{id}` | `EMPLOYEE_DELETE` |

## Projects (`/projects`)
| Method | Path | Auth |
|---|---|---|
| GET | `/projects` \| `/{id}` \| `/dashboard` | `PROJECT_READ` |
| POST | `/projects` | `PROJECT_CREATE` |
| PUT | `/projects/{id}` \| `/status` \| `/priority` \| `/timeline` | `PROJECT_UPDATE` |
| POST/DELETE | `/projects/{id}/members[/{employeeId}]` | `PROJECT_UPDATE` |
| DELETE | `/projects/{id}` | `PROJECT_DELETE` |

## Tasks (`/tasks`)
| Method | Path | Auth |
|---|---|---|
| GET | `/tasks` \| `/{id}` \| `/{id}/history` | `TASK_READ` |
| POST | `/tasks` | `TASK_CREATE` |
| PUT | `/tasks/{id}` \| `/status` \| `/priority` \| `/due-date` | `TASK_UPDATE` |
| PUT | `/tasks/{id}/assign` | `TASK_ASSIGN` |
| DELETE | `/tasks/{id}` | `TASK_DELETE` |

## Comments (`/comments`)
| Method | Path | Auth |
|---|---|---|
| GET | `/comments?ownerType=&ownerId=` | `COMMENT_READ` |
| GET | `/comments/{id}/history` | `COMMENT_READ` |
| POST | `/comments` | `COMMENT_CREATE` |
| PUT | `/comments/{id}` | `COMMENT_UPDATE` (author only) |
| DELETE | `/comments/{id}` | `COMMENT_DELETE` (author or ADMIN/SUPER_ADMIN) |

## Attachments / File Management (`/attachments`)
| Method | Path | Auth |
|---|---|---|
| GET | `/attachments?ownerType=&ownerId=` | `ATTACHMENT_READ` |
| GET | `/attachments/{id}` \| `/{id}/download` | `ATTACHMENT_READ` |
| POST | `/attachments` (multipart) | `ATTACHMENT_UPLOAD` |
| DELETE | `/attachments/{id}` | `ATTACHMENT_DELETE` |

`ownerType` is one of `EMPLOYEE`, `PROJECT`, `TASK` — this one controller
serves employee documents, project attachments and task attachments.

## Notifications (`/notifications`)
Self-scoped to the authenticated user, no permission beyond being logged
in: history (paginated), `/unread-count`, `/{id}/read`, `/read-all`,
`/preferences` (GET/PUT).

## Audit Logs (`/audit-logs`)
`AUDIT_READ` required for all of: `/audit-logs?action=`,
`/audit-logs/entity/{entityName}/{entityId}`, `/audit-logs/users/{userId}`.

## Standard response conventions

- Every list endpoint returns `PageResponse<T>` — `content`, `pageNumber`,
  `pageSize`, `totalElements`, `totalPages`, `first`, `last`.
- Every error, from any layer, returns the shape documented in
  [`api-examples.md`](api-examples.md#error-response-shape).
- Pagination/sorting query params follow Spring's convention:
  `?page=0&size=20&sort=createdAt,desc`.
