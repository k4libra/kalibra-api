# AGENTS.md

This file provides guidance to AI coding agents (Claude Code, Codex, Cursor and others) when working with code in this repository. `CLAUDE.md` only imports this file and `MEMORY.md`, so this is the single source to edit.

## Memory
- Start by reading `MEMORY.md` to understand the project's status and decisions made.
- Upon completing a task, update: current status, key decisions (and the reasoning behind them), and pitfalls to avoid.
- Keep it concise (max. ~50 lines): summarize or remove information that is no longer relevant.
- If something becomes a permanent rule, propose moving it to `AGENTS.md` instead of keeping it in the memory file.
- Never store sensitive data (keys, tokens, personal information).

## Commands

There is no Maven wrapper; use a local `mvn` (3.9+) with JDK 25.

```bash
cp .env.example .env                  # set DB_PASSWORD and JWT_SECRET
docker compose up -d                  # PostgreSQL 17 only
set -a && . ./.env && set +a && mvn spring-boot:run   # the app reads its config from env vars

mvn clean verify                      # full build + tests (what CI runs, as `mvn -B test`)
mvn test -Dtest=CoursesControllerTest                 # one test class
mvn test -Dtest=CoursesControllerTest#shouldRejectWhenNotAuthenticated   # one test method
mvn test -Dtest=ArchitectureTest      # module boundaries (ArchUnit)
```

- The test suite needs no database: domain tests are plain JUnit, application/persistence tests use Mockito, controller tests use `@WebMvcTest`. Only running the app needs Postgres.
- Swagger UI: `http://localhost:8080/swagger-ui.html` (public). Call `sign-in` first; the JWT cookie is then sent automatically.
- If `mvn clean verify` fails with a missing `*Assembler`/`*Mapper` bean, the VS Code Java extension compiled into `target/` during the `clean` and wiped the MapStruct-generated classes. Re-run the build.

## Architecture

Modular monolith, one Spring Boot deployable. Each bounded context is a root package `com.kalibra.api.<bc>` (today `iam`, `curriculum`, `enrollment` and `progress`) with four DDD layers: `domain` (pure POJOs, no Spring/JPA), `application`, `infrastructure`, `interfaces`. `shared` holds cross-cutting config and inter-module contracts. `ArchitectureTest` enforces the boundaries; when adding a module, add its packages to rule 5 (`shared` must not depend on any BC).

Each module is implemented from a validated PlantUML class diagram, one per BC, generated from a YAML model outside this repo. Class names, endpoints, repository methods and HTTP errors come from that diagram. Ask before inventing pieces it does not define.

**Inside a module**
- CQRS-lite: commands and queries are data-only `record`s. `XxxCommandService`/`XxxQueryService` interfaces live in `domain/services` with an overloaded `handle(...)` per command or query. One `XxxCommandServiceImpl`/`XxxQueryServiceImpl` per aggregate lives in `application/internal`.
- Domain services (`MasteryGapAnalyzer`, `CourseIndicatorsCalculator`, `StudentAnonymizer`) are plain classes in `domain/services`, with no Spring annotation. The `XxxQueryServiceImpl` that uses them instantiates them. They only compute over what the module owns; data of other contexts (names, emails, verification stats) is added by the query service.
- Double repository: the `domain/repositories/XxxRepository` works with aggregates. `infrastructure/persistence/repositories/XxxRepositoryImpl` implements it with a Spring Data `XxxJpaRepository` plus a MapStruct `XxxJpaMapper`.
- MapStruct is the only mapper. REST uses `interfaces/rest/transform/*Assembler` and JPA uses `*JpaMapper`. Aggregates and JPA entities keep a `public` no-arg constructor because MapStruct needs it. Single-field VOs and `Optional` fields need explicit `default` converter methods, since MapStruct 1.6 has no `Optional` support.
- Aggregates are scoped by `holderId`, the JWT `sub`, which equals the `iam` user id (UUID string). Repositories filter by it to prevent IDOR, and a resource owned by someone else answers `404`.
- Each module has its own `XxxControllerAdvice`, scoped with `assignableTypes`, that maps domain exceptions to RFC 9457 `ProblemDetail`. `shared/interfaces/rest/GlobalExceptionHandler` extends `ResponseEntityExceptionHandler` and turns anything unmapped into a generic `500`.

**Between modules (in-process only, no HTTP)**
- Sync calls go through a provider facade (OHS). The interface is `<bc>/interfaces/acl/<Bc>ContextFacade` and the implementation is `<bc>/application/acl/<Bc>ContextFacadeImpl`, which delegates to the provider's Command/Query services, never to its repository. Contracts are neutral `record`s (JDK types only) in `shared/contracts/<provider>`. The consumer wraps the facade in `application/internal/outboundservices/acl/External<Provider>Service` and translates to its own VOs. Never import another module's `domain`/`application`/`infrastructure`. If two consumers declare the same `External<Provider>Service` class name, give each an explicit bean name, or startup fails.
- Async communication uses domain events (records, past tense, no `Event` suffix) published with `ApplicationEventPublisher`. Handlers use `@TransactionalEventListener(phase = AFTER_COMMIT)`. The publishing method must be `@Transactional`, or the listener never fires. Writes triggered from an `AFTER_COMMIT` listener need `@Transactional(propagation = REQUIRES_NEW)`. Resilience is self-healing in the consumer's read/write path, or a reconciliation `@Scheduled` job for costly effects (see `curriculum`'s `PendingIngestionReconciliationJob`).

**Persistence**
- One PostgreSQL database with one schema per module. `spring.flyway.enabled=false`. Instead, `shared/config/FlywayConfig` declares one Flyway instance per module, with its own history table `flyway_<bc>_<resource-plural>` and migrations in `db/migration/<bc>/`.
- `ddl-auto: validate`, so every entity change needs a migration. Never edit an applied migration; add a new version.

**Security** (`shared/config/SecurityConfig`, a single filter chain in every environment)
- The JWT is issued by `iam` and travels only in an httpOnly, `Secure`, `SameSite=Lax` cookie (`JwtCookieFactory`), never in headers or bodies. `JwtAuthenticationFilter` reads it and maps the `roles` claim to `ROLE_<name>` authorities. Roles are additive: `REGISTERED_USER` plus `STUDENT` (sign-up from `MOBILE_APP`) or `TEACHER` (`WEB_PLATFORM`).
- Role rules are `requestMatchers(...).hasAuthority("ROLE_...")` placed before `anyRequest().authenticated()`. Today `/api/v1/courses/**`, `/api/v1/teachers/**` and the teacher listings require `TEACHER`; `/api/v1/subtopic-masteries/**`, `/api/v1/practice-exercises/**`, `/api/v1/exercise-attempts/**` and `PUT /api/v1/student-preferences/me/daily-reminder` require `STUDENT`. `GET /api/v1/courses/*/student-progress` is declared before the `/courses/**` rule because students also use it; a more specific matcher must always come first.
- No session answers an empty `401` (entry point). A wrong role answers an empty `403` from a custom `accessDeniedHandler`. The default handler's `sendError` would re-dispatch to `/error`, where the JWT filter does not run, and the response would turn into `401`. `MockMvc` does not reproduce that, so verify role changes against the running app too.
- Controller tests use `@WebMvcTest(XController.class)` + `@Import({SecurityConfig.class, JwtAuthenticationFilter.class, <Assembler>Impl.class})` + `@TestPropertySource(properties = "kalibra.jwt.secret=...")` + `user("id").roles("TEACHER")`.

**Placeholders waiting on decisions** (keep them behind their interfaces)
- `PushReminderNotificationService` only logs; the push provider is undecided.
- The Adaptive Engine (FastAPI) is not wired yet. Its three ACLs are Spring beans with their final signatures whose bodies throw `UnsupportedOperationException`: `ExternalCurricularExtractionService` (material stays `PENDING_INGESTION`), `ExternalExerciseGenerationService` and `ExternalMasteryEstimationService` (generating or answering an exercise answers `500`). Wire them without changing their callers; unit tests mock them.
- `RedisGapMapCacheService` is an in-memory map behind `GapMapCacheService` until Redis is added.
- `FileSystemMaterialStorageService` is a provisional local adapter behind `MaterialStorageService`. Files never go to PostgreSQL; only an opaque `storageReference` is stored.

## Conventions

- All code, messages and logs are in English. No comments except for non-obvious technical constraints. Getters and setters are multi-line. Use explicit `try/catch` (no `@SneakyThrows`).
- Naming:
  - Aggregate roots have no `Aggregate` suffix.
  - Controllers use the plural of their REST resource (`CoursesController`), or the sub-resource name for a singleton or nested resource (`WorkspaceController` for `/teachers/me/workspace`). Advices and tests follow the controller name.
  - Tables are plural.
- REST:
  - Prefix `/api/v1`, plural kebab-case resources, no verbs. Non-CRUD actions are plural sub-resources (`POST /invitations/{id}/cancellations`); the only exception is `/authentication/sign-up|sign-in|sign-out`.
  - Resources of the authenticated user live under `/me`, and their `GET` never creates state.
  - Growing collections paginate with `?page&size` (`size` 1..100) and answer an envelope `{content, page, size, totalElements, totalPages}`.
- Git Flow (see README): `feature/*` branches start from `develop` and merge into it by PR. `release/*` starts from `develop` and merges into `main` (tagged) and back into `develop`. `main` only takes PRs. Branches are named after the ticket, e.g. `feature/KAL-36`.
- Commits use Conventional Commits, in English and lowercase, without a body or `Co-Authored-By`: `type(scope): subject`, e.g. `feat(curriculum): add teacher workspace to switch the active course`. The scope is the module; group commits by purpose.
