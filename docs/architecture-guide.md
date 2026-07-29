# Architecture Guide

## 1. Layered architecture

Every module in this codebase follows the same four layers, and nothing
skips a layer:

```
Controller  →  Service (interface + impl)  →  Repository  →  Entity
     ↓               ↓
    DTO           Mapper
```

- **Controller** — HTTP concerns only: routing, `@Valid` request binding,
  `@PreAuthorize`, status codes. Never touches a repository directly.
- **Service** — business logic, transaction boundaries (`@Transactional`),
  cross-entity orchestration. Always programmed against an interface so
  it can be mocked in tests and, in principle, swapped (e.g. a
  `CachingUserService` decorator) without touching callers.
- **Repository** — Spring Data JPA interfaces, plus `Specification`
  classes in `repository/spec/` for search/filter/pagination that would
  otherwise require a combinatorial explosion of derived query methods.
- **Entity** — JPA-mapped persistence model. Entities are never returned
  from a controller; `Mapper` classes translate to/from DTOs at the
  service boundary, which is what keeps the persistence model free to
  change shape without breaking the public API contract.

## 2. Package structure

```
com.controlplane.backend
├── config          # Spring configuration: security, JPA auditing, Redis, Kafka, OpenAPI, health
├── controller       # REST controllers, one per module
├── service          # Business logic interfaces, with implementations in service/impl
├── repository        # Spring Data JPA repositories + repository/spec specifications
├── entity              # JPA entities and the enums package beneath it
├── dto                   # Request/response DTOs, one subpackage per module
├── mapper                 # Manual entity <-> DTO mappers
├── security                 # JWT provider/filter, UserDetails, security handlers
├── validation                 # Custom Bean Validation annotations and validators
├── exception                    # Domain exceptions + GlobalExceptionHandler
├── event                          # Kafka event payloads (event/) and consumers (event/listener/)
├── logging                          # Correlation id and request/response logging filters
└── docs (this folder)                  # Everything you're reading right now
```

## 3. Why entities are never exposed over the API

Three concrete reasons, not just "best practice":

1. **Lazy loading leaks.** A JPA entity serialized directly by Jackson
   will either trigger N+1 queries as Jackson walks lazy associations, or
   throw `LazyInitializationException` once the Hibernate session is
   closed — both are runtime surprises DTOs eliminate entirely.
2. **Schema changes shouldn't be API breaking changes.** Renaming a
   column, splitting a table, or adding a new required association are
   all persistence-layer decisions. Because `EmployeeResponse` is a
   separate, hand-maintained shape from the `Employee` entity, the
   mapper absorbs that change and the API contract stays stable.
3. **Field-level authorization.** `User.password` and `RefreshToken.token`
   should never leave the server under any circumstances; a DTO makes
   that a compile-time guarantee (the field simply doesn't exist on
   `UserResponse`) rather than a runtime `@JsonIgnore` someone could
   forget.

## 4. Multi-tenancy model

`Organization` is the tenant root. Every `Department` belongs to exactly
one organization (`departments.organization_id`, `NOT NULL`), and
`Employee` also carries a direct `organization_id` for the same reason —
so employee queries don't have to join through `department` (which is
nullable) just to scope by tenant. `Project` and `Task` are scoped
transitively through `Department`. `department.code` and
`employee.employeeCode` uniqueness is enforced per-organization, not
globally (`uq_departments_org_code`), which is what actually makes
multi-tenancy real rather than cosmetic — two different customers can
both have a department coded `ENG` without colliding.

## 5. Event-driven side effects (Kafka)

Two things happen on Kafka: `NotificationEvent` (something a specific
user should be told about) and `AuditEvent` (something that happened,
for compliance/history). Both are published fire-and-forget from the
originating service (see `AuthServiceImpl.login`, for example) and
consumed by a dedicated listener (`NotificationEventListener`,
`AuditEventListener`) that does the actual persistence.

This indirection is deliberate, not decorative: a login request
shouldn't fail or slow down because the audit table is briefly locked by
a concurrent write, and a task assignment shouldn't fail because an
email provider is down. The event pipeline absorbs that failure domain —
see `docs/security-architecture.md` and `KafkaConsumerConfig` for how
retries and the dead-letter topic handle a listener that itself fails.

## 6. Caching strategy

Redis caches are scoped per-entity (`users`, `employees`, `projects`,
`tasks`, `organizations`, `departments`) plus a short-TTL `dashboards`
cache for the aggregate queries that are expensive to compute and
tolerate a couple of minutes of staleness. Every mutating service method
carries a matching `@CacheEvict` (see `RedisCacheConfig` for the TTL
table) — the rule followed throughout the codebase is: if a method can
change what a cached `getById` would return, it evicts that cache key.

## 7. Where to look for more detail

- [`docs/database-design.md`](database-design.md) — schema, normalization, indexing rationale
- [`docs/security-architecture.md`](security-architecture.md) — JWT/RBAC/refresh token flows
- [`docs/api-examples.md`](api-examples.md) / [`docs/api-guide.md`](api-guide.md) — endpoint reference
- [`docs/er-diagram.md`](er-diagram.md) — full entity relationship diagram
- [`docs/sequence-diagrams.md`](sequence-diagrams.md) — event-driven flows
- [`docs/developer-guide.md`](developer-guide.md) — local setup and how to add a new module
- [`docs/deployment-guide.md`](deployment-guide.md) — AWS EC2 deployment
