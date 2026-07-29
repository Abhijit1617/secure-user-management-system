# Interview Guide

Everything below describes what's actually in this codebase — no
aspirational claims. If you can't point to the specific file backing a
statement here, don't say it in an interview either.

## How to explain this project in 2 minutes

"I built a backend for an internal company platform — think of it as
the system a mid-size company would use to manage its org chart,
projects, and day-to-day task tracking. It's multi-tenant: an
`Organization` owns its `Department`s, which own `Employee`s, who get
assigned to `Project`s and `Task`s. On top of that there's full
authentication with JWT access tokens and rotating refresh tokens,
role-and-permission-based authorization, comments with @mentions on
tasks, file attachments, and an event-driven side: when something
happens — a task gets assigned, someone logs in — that gets published to
Kafka, and separate consumers turn it into an in-app notification, an
email, or an audit log entry, so the request that triggered it doesn't
have to wait on any of that. Redis caches the read-heavy lookups. It's
containerized with Docker Compose, sits behind Nginx, and has a GitHub
Actions pipeline that builds and tests it on every push."

That's the whole thing in one breath. Everything below is what you'd say
if asked to go one level deeper on any piece of it.

## Project purpose

A portfolio project scoped like a real internal tool, not a CRUD tutorial:
the goal was to practice the patterns a backend developer actually uses
day to day — layered architecture, proper authZ, database migrations,
caching, async messaging, and the DevOps to actually run it — rather than
to build something with a novel idea. The interesting part is the
engineering, not the product.

## Architecture

Strict four-layer separation: **Controller → Service → Repository →
Entity**, with DTOs and mapper classes at the service boundary so an
entity is never serialized straight to JSON. See
[`architecture-guide.md`](architecture-guide.md) for the full package
breakdown. If asked "why not just return the entity," the answer is
lazy-loading exceptions, uncontrolled schema coupling, and accidentally
leaking fields like `password` — a DTO makes all three impossible by
construction, not by convention.

## Authentication / JWT flow

Login returns two tokens: a short-lived (15 min) signed JWT access token
carrying the user's roles and permissions as claims, and a long-lived,
opaque, server-side-tracked refresh token. The access token is
self-contained — `JwtAuthenticationFilter` validates its signature and
builds the security principal directly from its claims, no database hit
per request. The refresh token is the opposite: it's a random string
with no embedded data, looked up in `refresh_tokens` on every use, and
**rotated** — presenting one issues a new one and revokes the old one
immediately. If an already-revoked refresh token gets presented again,
that's treated as a stolen-token replay and every active session for
that user is revoked. Full sequence diagrams are in
[`security-architecture.md`](security-architecture.md).

**Likely follow-up: "Why not just make the access token long-lived and
skip refresh tokens entirely?"** Because a JWT can't be revoked before it
expires — if one leaks, you're stuck waiting out its lifetime. Keeping
the access token short and putting revocability in the refresh token
(which lives in the database and can be deleted) is the standard
trade-off.

## Database design

PostgreSQL, normalized to 3NF, with Flyway managing every schema change
as a numbered, immutable migration (`V1` through `V9`). `Organization`
is the tenant root; department codes and employee codes are unique
*per organization*, not globally, which is what actually makes
multi-tenancy real. Two tables — `comments` and `attachments` — use a
polymorphic `owner_type`/`owner_id` pair instead of a foreign key,
because they can point at more than one kind of parent (a task, a
project, or an employee) and Postgres has no conditional foreign key.
Full details in [`database-design.md`](database-design.md) and
[`er-diagram.md`](er-diagram.md).

## Organization hierarchy

`Organization → Department (self-referential, so departments can nest)
→ Employee`. An employee links to a `User` (the login identity) via a
one-to-one relationship — they're kept separate because not every user
is necessarily an employee (a service account, an external auditor)
and, conceptually, "who can log in" and "who works here" are different
questions even though they usually overlap.

## Project/task workflow

`Project` belongs to a `Department`, has a manager and a set of member
`Employee`s, a status (`PLANNED → IN_PROGRESS → ... → COMPLETED`), and a
priority. `Task` belongs to a `Project`, has an assignee and reporter,
and every status/priority/assignee/due-date change is written to
`task_history` — so "what happened to this task" is a real, queryable
audit trail, not just the current state.

## Redis usage

Six cache regions (`users`, `employees`, `projects`, `tasks`,
`organizations`, `departments`) plus a short-TTL `dashboards` cache for
the aggregate project-dashboard query. Every `getById` is `@Cacheable`;
every method that could change what that lookup returns carries a
matching `@CacheEvict`. TTLs are deliberately different per region —
organizations change rarely (30 min TTL), dashboards are expensive to
compute but fine to be a couple minutes stale (2 min TTL). See
`RedisCacheConfig` for the exact table.

## Kafka usage

Two event types: `NotificationEvent` (something a specific user should
be told about) and `AuditEvent` (something happened, for the audit
trail). Both are published fire-and-forget from the originating service
and picked up by a dedicated `@KafkaListener`. Every listener shares one
`DefaultErrorHandler`: a failing message retries a few times with
exponential backoff, then gets published to `<topic>.DLT` instead of
blocking the partition or getting silently dropped.

**Likely follow-up: "Why Kafka instead of just calling the
NotificationService directly?"** Because a slow email provider or a
momentarily locked audit table shouldn't make a task-assignment request
fail or hang. Kafka is the boundary that absorbs that failure domain — a
consumer can retry and eventually dead-letter without the original
request ever knowing or caring.

## Exception handling

Every custom exception extends `BusinessException`, which carries its
own `HttpStatus` — `GlobalExceptionHandler` doesn't need a giant
if/else, it just reads that field off whatever was thrown. Bean
Validation failures, malformed JSON, Spring Security's own exceptions
(`BadCredentialsException`, `AccessDeniedException`), and a genuine
catch-all for anything unexpected all funnel into the same
`ErrorResponse` shape, so a client only ever has to parse one error
format.

## Docker architecture

Multi-stage `Dockerfile` — a Maven-based build stage, then a slim JRE
runtime stage running as a non-root user. `docker-compose.yml` brings up
Postgres, Redis, Kafka+Zookeeper, the backend, and Nginx; only Nginx
publishes a port to the host, everything else stays on the internal
Docker network — the same shape a real deployment would have, just on
one machine instead of managed services.

## Important design decisions (and why)

- **Manual mappers instead of MapStruct.** Easier to read and debug for
  nested mappings like resolving an employee's manager's full name; see
  `developer-guide.md` for the fuller reasoning.
- **Refresh tokens are revoked, not deleted, at logout/rotation time** —
  keeping the row lets replay detection work; a scheduled job
  (`RefreshTokenCleanupScheduler`) purges them after they've been
  expired for 24 hours, so the table doesn't grow forever either.
- **Local disk file storage, not S3/Cloudinary**, for attachments — kept
  behind a `FileStorageService` interface specifically so swapping the
  backend later doesn't touch any calling code.

## Known limitations (say these honestly if asked)

- This has not been load-tested or run against real production traffic.
- Single-instance Postgres/Redis/Kafka in the current Docker Compose
  setup — no replication or failover; the migration path to managed
  services (RDS/ElastiCache/MSK) is documented but not implemented.
- No rate limiting beyond Nginx's basic `limit_req` — no per-user quota.
- The frontend doesn't exist yet; this is backend-only.
- A handful of repository query methods (e.g.
  `OrganizationRepository.findBySlug`) are written but not yet wired to
  an endpoint — reasonable API surface for a repository, but worth
  knowing they're there if asked to justify every line.

## Likely interviewer questions and answers

**"Walk me through what happens when someone logs in."**
`AuthController` → `AuthServiceImpl.login()`: look up the user, check
lock status, verify the password with BCrypt, check email verification,
issue a signed JWT access token plus a new refresh token row, publish a
`LOGIN` audit event to Kafka, return both tokens. A wrong password
increments `failed_login_attempts`; hitting the configured max (default
5) locks the account for 30 minutes and emails the user.

**"How do you handle authorization beyond just 'is this user logged
in'?"** Every role carries a set of permissions (e.g. `TASK_ASSIGN`,
`ORGANIZATION_DELETE`) seeded via Flyway and granted at login time as
JWT claims. Controllers check `@PreAuthorize("hasAuthority('X')")`, so
authorization is a pure in-memory check against the token's claims —
no database round trip needed per request.

**"What would you do differently if you rebuilt this?"** Probably
introduce a proper outbox pattern for the Kafka publishing instead of
firing `kafkaTemplate.send()` directly from the service method — right
now there's a small window where the database transaction commits but
the Kafka publish fails silently (logged, not retried at the app level).
An outbox table written in the same transaction as the business change,
polled by a separate publisher, closes that gap.

**"Why PostgreSQL over MySQL, or SQL over NoSQL?"** The data is
inherently relational — organizations, departments, employees, projects
and tasks all reference each other with real foreign-key constraints
that matter (you can't delete a department that still has employees).
Postgres specifically for `TIMESTAMPTZ`, `gen_random_uuid()`, and
general schema/tooling maturity with Flyway.
