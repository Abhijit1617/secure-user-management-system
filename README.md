# Backend Control Plane

A secure enterprise backend for managing organizations, departments,
employees, projects, tasks, comments, attachments, notifications and
audit trails — built as a production-style reference project rather than
a toy CRUD demo.

> **Status: v1.0.0.** Every planned module is implemented end to end:
> JWT authentication with refresh rotation and account lockout, RBAC
> with a role hierarchy, Organizations → Departments → Employees →
> Projects → Tasks with full CRUD, nested Comments with @mentions and
> edit history, file attachments, Redis caching, Kafka-driven
> notifications and audit logging with dead-letter handling, Actuator
> health/metrics, correlation-ID structured logging, a Docker image
> behind Nginx, and a GitHub Actions CI pipeline. This is a
> production-*style* architecture built to demonstrate those patterns
> correctly — it has not been load-tested or run against real traffic,
> and a few things a genuine production deployment would still need are
> called out honestly in
> [`docs/deployment-guide.md`](docs/deployment-guide.md#6-migrating-to-managed-services-later)
> (managed database/cache/broker services) and in
> [`docs/interview-guide.md`](docs/interview-guide.md) (known
> limitations). The React frontend is a separate, not-yet-started
> project.

## 1. What this is

Most portfolio projects stop at "CRUD with JWT." This one is scoped like
something a mid-size SaaS company would actually run internally: role and
permission based access control, audited writes, a normalized relational
schema with real foreign keys and indexes, and a package structure that
keeps concerns (web, persistence, security, messaging) from bleeding into
each other.

## 2. Tech stack

| Layer          | Technology                                      |
|-----------------|---------------------------------------------------|
| Language         | Java 21                                             |
| Framework        | Spring Boot 3.5, Spring Security, Spring Data JPA     |
| Database          | PostgreSQL 17, Flyway migrations                       |
| Cache             | Redis 8 (Spring Cache, per-region TTLs)                 |
| Messaging          | Apache Kafka (notification + audit events, DLQ, retry)   |
| Auth               | JWT (jjwt), BCrypt                                       |
| Docs               | springdoc-openapi (Swagger UI)                            |
| Object mapping      | Manual mapper classes (see `docs/developer-guide.md`)       |
| Testing             | JUnit 5, Mockito, Testcontainers                              |
| Build               | Maven                                                          |
| DevOps               | Docker, Docker Compose, Nginx, GitHub Actions, Prometheus, Grafana |

## 3. Package structure

```
com.controlplane.backend
├── config          # Spring configuration classes (security, JPA auditing, Kafka, cache, OpenAPI)
├── controller       # REST controllers, one per module
├── service          # Business logic, interfaces + implementations
├── repository       # Spring Data JPA repositories
├── entity           # JPA entities and the enums package beneath it
├── dto              # Request/response DTOs, kept separate from entities
├── mapper           # Manual entity <-> DTO mapper classes
├── security         # JWT provider, filters, user details service
├── validation        # Custom Bean Validation annotations and validators
├── exception          # Domain exceptions and the global exception handler
├── cache               # Cache key builders and Redis-backed helpers
├── audit               # Audit logging aspect and audit context
├── event                # Kafka event payloads and publishers/listeners
├── notification          # Notification generation and delivery
├── util                   # Stateless helpers shared across modules
└── scheduler               # Scheduled jobs (token cleanup, overdue task sweeps)
```

The guiding rule: controllers never touch repositories directly, services
never return entities across the API boundary, and mappers are the only
place entity <-> DTO conversion happens. This keeps the persistence model
free to evolve without breaking the public API contract.

## 4. Domain model

Organizations are structured as `Department -> Employee -> Project ->
Task`, with `User` as the authentication identity layered on top of
`Employee` (not every user has to be an employee — service accounts and
external auditors are users without an employee record). Full details,
including the ER diagram, normalization notes and indexing rationale, are
in [`docs/database-design.md`](docs/database-design.md).

Security architecture — JWT claims, the authentication and authorization
sequence diagrams, refresh token rotation and the role hierarchy — is
documented in [`docs/security-architecture.md`](docs/security-architecture.md).

## 5. Authentication & Authorization API

Once the application is running, Swagger UI at `/swagger-ui.html` documents
every endpoint below with request/response schemas and status codes. The
full JWT flow, refresh token rotation sequence and role hierarchy are
diagrammed in [`docs/security-architecture.md`](docs/security-architecture.md);
worked `curl` examples for every endpoint are in
[`docs/api-examples.md`](docs/api-examples.md).

| Method | Endpoint | Auth required | Purpose |
|--------|-----------|-----------------|-----------|
| POST | `/api/v1/auth/register` | No | Create an account (default `EMPLOYEE` role, pending email verification) |
| POST | `/api/v1/auth/login` | No | Exchange credentials for an access + refresh token |
| POST | `/api/v1/auth/refresh` | No | Rotate a refresh token for a new token pair |
| POST | `/api/v1/auth/logout` | No | Revoke a refresh token |
| GET | `/api/v1/auth/me` | Yes | Get the current user's profile, roles and permissions |
| POST | `/api/v1/auth/change-password` | Yes | Change password, revokes all other sessions |
| POST | `/api/v1/auth/forgot-password` | No | Request a password reset email |
| POST | `/api/v1/auth/reset-password` | No | Complete a password reset with the emailed token |
| GET | `/api/v1/auth/verify-email` | No | Verify an email address with the emailed token |
| POST | `/api/v1/auth/resend-verification` | No | Resend the verification email |
| DELETE | `/api/v1/auth/deactivate` | Yes | Deactivate your own account |
| PUT | `/api/v1/auth/reactivate/{userId}` | Yes (`USER_UPDATE`) | Administrator reactivates a deactivated account |
| `/api/v1/users/**` | — | Yes | User search/CRUD, soft delete, restore, enable/disable, role assignment |
| `/api/v1/roles/**` | — | Yes (`SUPER_ADMIN`) | Role CRUD and permission assignment |
| `/api/v1/permissions/**` | — | Yes (`SUPER_ADMIN`) | Permission CRUD |

## 6. Prerequisites

- JDK 21
- Maven 3.9+
- Docker and Docker Compose
- (Optional) an IDE with Lombok and annotation processing enabled

## 7. Running the project locally

**1. Start the infrastructure (PostgreSQL, Redis, Kafka):**

```bash
docker compose up -d
```

This brings up:

- PostgreSQL 17 on `localhost:5432` (db `control_plane_db`, user
  `control_plane_user`, password `control_plane_pass`)
- Redis 8 on `localhost:6379`
- Kafka on `localhost:9092` (plus Zookeeper and a Kafka UI on
  `localhost:8090` for inspecting topics)

**2. Configure environment variables (optional for local dev):**

The defaults in `application.yml` already match the Docker Compose
credentials above, so local development works with zero configuration.
For anything beyond local dev, copy the variables below into your shell
or a `.env` file consumed by your process manager:

```bash
DB_URL=jdbc:postgresql://localhost:5432/control_plane_db
DB_USERNAME=control_plane_user
DB_PASSWORD=control_plane_pass
REDIS_HOST=localhost
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
JWT_SECRET=replace-with-a-real-256-bit-secret-before-deploying
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

**3. Run database migrations and start the application:**

```bash
./mvnw spring-boot:run
```

Flyway runs automatically on startup and applies:

- `V1__init_schema.sql` — full baseline schema
- `V2__seed_roles_and_permissions.sql` — `SUPER_ADMIN`, `ADMIN`,
  `MANAGER`, `EMPLOYEE`, `VIEWER` roles with their permission grants

**4. Verify it's up:**

```bash
curl http://localhost:8080/actuator/health
```

Swagger UI is available at `http://localhost:8080/swagger-ui.html` once
the app is running, documenting every endpoint across all modules.

## 8. Running tests

```bash
./mvnw test
```

Integration tests that need a real database use Testcontainers, so Docker
must be running locally, but no manually started database is required —
Testcontainers spins up and tears down its own PostgreSQL container per
test class.

## 9. Building a production jar

```bash
./mvnw clean package -DskipTests
java -jar target/backend-control-plane.jar --spring.profiles.active=prod
```

## 10. Running the full stack with Docker (production-equivalent)

```bash
cp .env.example .env    # fill in real secrets — see the file for what's required
docker compose up -d --build
```

This builds the backend from the production multi-stage `Dockerfile` and
starts Postgres, Redis, Kafka (+ Zookeeper + Kafka UI on port 8090), the
backend, Nginx as a reverse proxy on port 80, and a Prometheus + Grafana
monitoring stack. Only the `backend` container itself publishes no host
port — every request to it goes through Nginx. Postgres, Redis, Kafka
and the observability tools do publish host ports (5432, 6379, 9092,
8090, 9090, 3001) purely for local development convenience (connecting
with `psql`, a Redis client, or a browser); on a real deployment those
would be firewalled at the security-group/network level rather than
routed through Nginx — see
[`docs/deployment-guide.md`](docs/deployment-guide.md) for exactly which
ports should stay internal-only in production.

```bash
curl http://localhost/actuator/health
```

**Monitoring**: Prometheus scrapes `/actuator/prometheus` every 15s
(config in `monitoring/prometheus/prometheus.yml`); Grafana is
auto-provisioned with Prometheus as its datasource and a starter
dashboard (JVM memory, CPU, HTTP request rate, thread count, uptime — see
`monitoring/grafana/dashboards/backend-control-plane-overview.json`).
Open `http://localhost:3001` and log in with the `GRAFANA_ADMIN_USER`/
`GRAFANA_ADMIN_PASSWORD` you set in `.env`.

**Logging**: `src/main/resources/logback-spring.xml` extends Spring
Boot's own default console configuration (so `logging.pattern.console`
in dev and `logging.structured.format.console: ecs` — JSON — in prod
keep working unchanged) and adds a rolling file appender for the `prod`
profile only, writing to `logs/backend-control-plane.log` with the
rollover policy configured in `application-prod.yml`.

For a real AWS EC2 deployment, TLS setup, and the managed-services
migration path, see [`docs/deployment-guide.md`](docs/deployment-guide.md).
CI (build, test, package, Docker image build) runs automatically on every
push via [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

## 11. Documentation index

| Doc | Covers |
|---|---|
| [`docs/architecture-guide.md`](docs/architecture-guide.md) | Layered architecture, package structure, multi-tenancy, caching/event design decisions |
| [`docs/database-design.md`](docs/database-design.md) | Core schema, normalization, indexing rationale |
| [`docs/er-diagram.md`](docs/er-diagram.md) | Complete ER diagram including every module |
| [`docs/security-architecture.md`](docs/security-architecture.md) | JWT/RBAC/refresh-token sequence diagrams |
| [`docs/sequence-diagrams.md`](docs/sequence-diagrams.md) | Kafka notification, audit + DLQ, and comment-mention flows |
| [`docs/api-guide.md`](docs/api-guide.md) | Every endpoint, grouped by module, with required permission |
| [`docs/api-examples.md`](docs/api-examples.md) | Worked `curl` examples |
| [`docs/developer-guide.md`](docs/developer-guide.md) | Conventions and how to add a new module |
| [`docs/deployment-guide.md`](docs/deployment-guide.md) | AWS EC2 deployment, TLS, managed-services migration |
| [`docs/git-commit-plan.md`](docs/git-commit-plan.md) | The phase-by-phase build order this project actually followed |

## 12. What's left

This backend is feature-complete and production-configured. Two things
remain outside this repository's scope:

- **The React frontend** — not part of this backend repository.
- **Managed AWS services** (RDS, ElastiCache, MSK) — the current
  deployment runs Postgres/Redis/Kafka as containers alongside the
  backend on one EC2 instance, which is the right starting point; the
  migration path to managed services, which requires no application code
  changes, is documented in
  [`docs/deployment-guide.md`](docs/deployment-guide.md#6-migrating-to-managed-services-later).
