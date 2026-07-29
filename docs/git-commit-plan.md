# Git Commit Plan

This plan lays out how the project's history should be built up commit by
commit, so the repository reads like the real, incremental work of one
developer rather than a single bulk import. Each phase below corresponds
to a small number of commits; commit messages follow
[Conventional Commits](https://www.conventionalcommits.org/).

## Phase 1 — Project foundation (current phase)

```
chore: initialize maven project with spring boot 3.5 and java 21
chore: add docker compose for postgres, redis and kafka
feat: add base entity with auditing and optimistic locking
feat: add rbac entities (role, permission)
feat: add user entity and enums
feat: add department and employee entities
feat: add project and task entities
feat: add notification, audit log and refresh token entities
feat: add jpa auditing configuration
chore: add flyway baseline schema migration
chore: seed default roles and permissions
docs: add database design documentation with er diagram
docs: add security architecture documentation
docs: add readme with installation guide
```

## Phase 2 — Security and authentication

```
feat: add jwt token provider and signing configuration
feat: add custom user details service
feat: add jwt authentication filter
feat: add security configuration and filter chain
feat: add role hierarchy configuration
feat: implement login endpoint
feat: implement refresh token rotation
feat: implement logout and token revocation
feat: implement forgot password flow with redis backed tokens
feat: implement reset password endpoint
feat: implement email verification flow
test: add authentication service unit tests
test: add authentication controller integration tests
```

## Phase 3 — Core domain modules

```
feat: add department module (dto, mapper, service, controller)
feat: add employee module with pagination and filtering
feat: add project module with member assignment
feat: add task module with status transitions
feat: add global exception handling
feat: add request validation groups
docs: add swagger annotations to domain controllers
test: add unit tests for department, employee, project and task services
test: add integration tests for domain controllers
```

## Phase 4 — Notifications, audit and caching

```
feat: add notification module and unread count endpoint
feat: add audit logging aspect for write operations
feat: add redis caching for dashboard aggregates
feat: add kafka producer configuration
feat: publish domain events (user created, task assigned, project created)
feat: add kafka consumer for notification generation
test: add tests for notification and audit modules
```

## Phase 5 — Profile, settings and file storage

```
feat: add profile module (view and update own profile)
feat: add settings module for platform configuration
feat: add local disk file storage for employee documents, task and project attachments
test: add tests for profile and settings modules
```

## Phase 6 — Frontend

```
chore: scaffold react 19 + typescript + vite project
feat: add axios client with interceptor based token refresh
feat: add authentication pages and protected routes
feat: add dashboard layout with sidebar and navbar
feat: add employee management screens
feat: add project and task board screens
feat: add profile and settings screens
style: apply tailwind design system
```

## Phase 7 — DevOps and delivery

```
chore: dockerize backend service
chore: dockerize frontend with nginx
chore: finalize docker compose for full stack local development
ci: add github actions workflow for build and test
docs: add deployment guide for aws ec2
docs: add interview preparation notes
```

## Conventions used throughout

- `feat:` — a new capability visible to API consumers or users.
- `fix:` — a bug fix.
- `chore:` — tooling, dependencies, configuration with no behavior change.
- `docs:` — documentation only.
- `test:` — tests only, no production code change.
- `refactor:` — internal restructuring with no behavior change.
- `style:` — formatting or purely visual/frontend styling changes.
- `ci:` — continuous integration configuration.

Commits stay small and scoped to one logical change so that `git log` and
`git blame` remain useful tools rather than noise, and so a reviewer can
follow the reasoning behind the architecture one commit at a time.
