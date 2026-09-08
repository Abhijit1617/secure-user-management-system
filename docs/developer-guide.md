# Developer Guide

## 1. Local setup

See the root [`README.md`](../README.md) for the quickest path to running
the project. This guide covers everything past "it's running" — coding
conventions and how to extend it.

## 2. Coding conventions

- **Constructor injection only.** Every service/controller/component uses
  `@RequiredArgsConstructor` on `private final` fields. No `@Autowired`
  field injection anywhere in the codebase.
- **Interface + impl for services.** `service/FooService.java` (interface)
  + `service/impl/FooServiceImpl.java`. Controllers and other services
  depend on the interface.
- **DTOs live under `dto/<module>/`.** Request DTOs use Bean Validation
  annotations directly on the fields; response DTOs are plain `@Getter
  @Builder` records of data, no validation.
- **Mappers are manual, not MapStruct.** Every mapper is a plain
  `@Component` with explicit `toResponse(...)` methods. This was a
  deliberate choice over annotation-processor-generated mapping — it's
  easier to debug, and the mapping logic for nested references (e.g.
  `EmployeeMapper` resolving `reportingManager.getUser().getFullName()`)
  is easier to read as real code than as MapStruct expression strings.
- **Every mutating method logs at INFO.** `log.info("Created project
  [{}] in department [{}]", ...)` — one line per state change, using the
  natural business key (code, username, slug) rather than the UUID,
  since that's what a human reading the log actually wants.
- **Exceptions extend `BusinessException`.** Never throw a raw
  `RuntimeException` from a service; add a new subclass in `exception/`
  if none of the existing ones fit, so `GlobalExceptionHandler` can map
  it to the right HTTP status automatically.

## 3. Adding a new module (worked example)

Say you're adding a "Timesheet" module. The order that keeps every layer
compiling as you go:

1. **Entity** (`entity/Timesheet.java`) extending `BaseEntity`, plus a
   Flyway migration (`V<next>__add_timesheets.sql`) — check
   `src/main/resources/db/migration/` for the next version number.
2. **Repository** (`repository/TimesheetRepository.java`), plus a
   `repository/spec/TimesheetSpecifications.java` if it needs
   search/filter.
3. **DTOs** (`dto/timesheet/`) — at minimum a `CreateTimesheetRequest`,
   `TimesheetResponse`, and if you need list views, a lighter
   `TimesheetSummaryResponse`.
4. **Mapper** (`mapper/TimesheetMapper.java`).
5. **Service** (`service/TimesheetService.java` interface +
   `service/impl/TimesheetServiceImpl.java`). Add `@Cacheable`/
   `@CacheEvict` here if the entity is read far more than it's written —
   see `RedisCacheConfig` for how to register a new cache region.
6. **Controller** (`controller/TimesheetController.java`) with
   `@PreAuthorize("hasAuthority('TIMESHEET_READ')")` etc. — remember to
   seed the new permissions in a migration (`V2__seed_roles_and_permissions.sql`
   already ran, so follow the pattern in `V5`/`V7` of inserting new
   permissions and granting them to existing roles rather than editing
   `V2`).
7. **Tests** — a `TimesheetServiceImplTest` with Mockito is the minimum
   bar; see `TaskServiceImplTest` for the shape (mock every repository
   the service depends on, verify both the happy path and at least one
   business-rule rejection).

## 4. Testing conventions

- Unit tests (`service/impl/*Test.java`) use `@ExtendWith(MockitoExtension.class)`
  with `@Mock`/`@InjectMocks`, never a Spring context — they should run
  in milliseconds.
- Repository tests (`repository/*Test.java`) use `@DataJpaTest` with
  Testcontainers' real PostgreSQL, not H2 — the schema uses Postgres-
  specific types (`TIMESTAMPTZ`, `gen_random_uuid()`) that H2 doesn't
  support, and Flyway runs the real migrations against the container so
  a broken migration fails the test suite, not just production.
- Controller tests (`controller/*Test.java`) use `@WebMvcTest` with
  `@AutoConfigureMockMvc(addFilters = false)` — they test request
  validation and response shape, not the security filter chain.

## 5. Database migrations

Never edit a migration that has already been committed — Flyway
checksums every applied migration and refuses to start if one changes
underneath it. Always add a new `V<n>__description.sql` file instead,
following the pattern already established: schema changes and data
backfills are separate migrations from permission seeding (see how `V4`
adds the `organizations` table and `V5` seeds its permissions
separately).

## 6. Running the full stack locally with Docker

```bash
cp .env.example .env
docker compose up -d --build
```

This builds the `backend` image from the production `Dockerfile` and
brings up Postgres, Redis, Kafka, Kafka UI, the backend, and Nginx as a
reverse proxy on port 80. Useful when you want to sanity check the exact
artifact that would ship to production rather than running via
`spring-boot:run`.
