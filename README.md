# Kalibra API

![CI](https://github.com/k4libra/kalibra-api/actions/workflows/ci.yml/badge.svg)

## Summary

Kalibra API is the backend for the Kalibra platform, built with Java, the Spring
Boot Framework, and Spring Data JPA on a PostgreSQL database, following
Domain-Driven Design. Each bounded context lives as an internal module inside a
single deployable, and contexts communicate in-process through domain events
rather than over the network.

## Features

- RESTful API
- Domain-Driven Design (modular monolith)
- Own JWT authentication (locally issued by the IAM module)
- Spring Boot Framework
- Spring Data JPA
- Bean Validation
- PostgreSQL Database (schema per module)
- Flyway Migrations (per module)
- In-process Domain Events
- Scheduled jobs (Spring `@Scheduled`, UTC)
- OpenAPI documentation (springdoc, Swagger UI)
- ArchUnit boundary enforcement
- Health endpoint (Spring Boot Actuator)
- Global fallback exception handler
- Continuous Integration (GitHub Actions)
- Git Flow branching strategy (`main` protected, `develop`, `feature/*`, `release/*`)

## Bounded Contexts

The application is divided into internal modules, each one a bounded context with
its own domain, application, infrastructure, and interfaces layers.

### Identity and Access Management (IAM) Context

The IAM Context is responsible for account access and the personal settings of each
user. It includes the following features:

- Register a new user (sign-up) with a securely hashed password. The client application
  decides the role: `MOBILE_APP` registers a `STUDENT`, `WEB_PLATFORM` registers a `TEACHER`.
- Enforce unique email addresses across accounts, including concurrent sign-ups (`409`).
- Authenticate a user (sign-in) and issue a locally signed JWT in an httpOnly cookie.
- Sign-in failures answer a single generic `401` (never revealing whether the email
  exists) as an RFC 9457 `ProblemDetail`.
- Sign out by clearing the httpOnly cookie server-side.
- Role-based access control (RBAC) with accumulable roles (`REGISTERED_USER`,
  `STUDENT`, `TEACHER`, `ADMINISTRATOR`). Roles are additive, not exclusive — an account
  can hold several at once. Every new account gets `REGISTERED_USER` plus the role of its
  client application; granting `ADMINISTRATOR` never removes them. Roles travel in the
  JWT `roles` claim and land as `ROLE_<name>` authorities via `JwtAuthenticationFilter` —
  protect an endpoint with `.hasAuthority("ROLE_ADMINISTRATOR")` in `SecurityConfig` (see
  the example comment there). There is no built-in endpoint to grant a role — that flow is
  decided by the Kalibra product requirements as the application evolves.
- Student preferences: daily study reminder (enabled + time, in **UTC**) and dark mode.
  `GET /student-preferences/me` never creates anything — it answers the defaults until the
  first `PUT`, which creates the record (idempotent upsert). Clients convert the local
  reminder time to UTC before sending it.
- `DailyStudyReminderJob` runs every minute (UTC) and notifies the students whose reminder
  is due through `ReminderNotificationService`. The current implementation
  (`PushReminderNotificationService`) only logs; the mobile push provider is still to be
  defined.
- Exposes the `IamContextFacade` (OHS) so other contexts can ask, in-process, whether an
  email belongs to a student and fetch a user summary.

On successful registration it publishes the `UserRegistered` domain event, allowing
other contexts to react in-process while IAM stays decoupled from them. The event is
audit-only: nothing subscribes to it.

## Technology Stack

| Concern            | Technology                               |
| ------------------ | ---------------------------------------- |
| Language           | Java 25                                  |
| Framework          | Spring Boot 3.5.15                       |
| Persistence        | Spring Data JPA · PostgreSQL 17 · Flyway |
| Mapping            | MapStruct 1.6.3                          |
| Security           | Spring Security · JWT (jjwt 0.12.6)      |
| API documentation  | springdoc-openapi 2.9.1 (Swagger UI)     |
| Architecture tests | ArchUnit 1.4.1                           |

Lombok, Flyway, and the PostgreSQL driver are managed by the `spring-boot-starter-parent` BOM.

## Project Structure

```
com.kalibra.api
├── iam/        Generic — authentication and personal settings. User aggregate
│               (Email + HashedPassword VOs), StudentPreferences aggregate, issues its own
│               JWT, publishes the UserRegistered event and exposes IamContextFacade.
└── shared/     Cross-cutting configuration (Flyway per module, JWT security) and the
                inter-module contracts (shared/contracts).
```

Inter-module communication goes through in-process domain events (`UserRegistered`)
and the `IamContextFacade` Open Host Service (`iam/interfaces/acl`), whose contract types
live in `shared/contracts/iam`.

## Getting Started

### Prerequisites

- JDK 25
- Docker (PostgreSQL 17)
- Maven 3.9+

### Configuration

```bash
cp .env.example .env   # set DB_PASSWORD and JWT_SECRET (openssl rand -base64 64)
```

### Running the application

```bash
docker compose up -d   # starts PostgreSQL 17 only
mvn spring-boot:run    # or run the application from the IDE
```

Flyway creates the `iam` schema and its tables on startup.

### API documentation

With the application running, Swagger UI is available at
`http://localhost:8080/swagger-ui.html` and the OpenAPI document at `/v3/api-docs`
(both public). Authentication endpoints need no credentials; for the protected ones,
call `sign-in` with "Try it out" first — the browser stores the JWT cookie and sends it
on every later call.

## Git Workflow

<p align="justify">
This project follows a lightweight Git Flow. <code>main</code> only ever holds
deployable code — nobody pushes to it directly, and no work happens on it beyond
merging a finished <code>release/*</code> (or an urgent <code>hotfix/*</code>). Every
merge into <code>main</code> is a deploy trigger, so it stays tagged with the version
it represents (e.g. <code>v1.2.0</code>).
</p>

<p align="justify">
<code>develop</code> is the integration branch. It branches off <code>main</code> once,
at the start of the project, and every <code>feature/*</code> branch is cut from it and
merged back into it via pull request. This is where several features live and get
tested together before anything is considered for release, without ever touching
<code>main</code>.
</p>

<p align="justify">
<code>feature/*</code> branches are short-lived and scoped to a single task. They branch
off <code>develop</code> and merge back into <code>develop</code> once the change is
done and CI passes. Naming convention: <code>feature/&lt;short-description&gt;</code>
(e.g. <code>feature/refresh-token</code>).
</p>

<p align="justify">
<code>release/*</code> branches are cut from <code>develop</code> once a set of features
is ready to ship. No new features land here — only the stabilization fixes needed to get
that specific batch production-ready (config tweaks, last-mile bug fixes). Naming
convention: <code>release/&lt;version&gt;</code> (e.g. <code>release/1.2.0</code>). When
it's ready, it merges into both <code>main</code> (tagged and deployed) and
<code>develop</code> (so the stabilization fixes aren't lost).
</p>

<p align="justify">
<code>hotfix/*</code> branches are the emergency exception: cut directly from
<code>main</code> to patch a production issue that can't wait for the next release,
then merged back into both <code>main</code> and <code>develop</code>.
</p>

```
main        ●───────────────────────────●─────────────●
                                          \ (tag v1.1)   \ (tag v1.2)
develop      ●───●───────●───●───●───●────●─────────●────●
                  \       \          \    release/1.2.0
              feature/x  feature/y  feature/z
```

<p align="justify">
This repository starts with only <code>main</code>, since it is the active Kalibra
project and not a generated starter. The team should create <code>develop</code> right
away — <code>git checkout -b develop && git push -u origin develop</code> — before
opening the first <code>feature/*</code> branch. <code>main</code> is protected: it only
accepts pull requests, each requiring the CI <code>build</code> job to pass.
</p>

## Security

JWT authentication is enabled by default (`shared/config/SecurityConfig`), in every
environment — there is no permit-all development mode. `/api/v1/authentication/**`
stays public (sign-up/sign-in/sign-out); every other endpoint requires a valid JWT and
answers `401` when the cookie is missing or invalid.

The JWT never travels in the response body or a header the client sets manually: on
sign-in, it's set as an **httpOnly, `SameSite=Lax` cookie** (`shared/config/JwtCookieFactory`),
so client-side JavaScript — and therefore XSS — can never read or exfiltrate it. The
browser attaches it automatically on later requests, and `JwtAuthenticationFilter`
reads it from the cookie rather than an `Authorization` header. Because JS cannot
delete an httpOnly cookie itself, `/sign-out` clears it server-side.

A browser frontend on a different origin (e.g. a Vite/React dev server) needs CORS
configured with credentials (`shared/config/CorsConfig`, `CORS_ALLOWED_ORIGIN` in
`.env`) and must call this API with `credentials: 'include'` (fetch) or
`withCredentials: true` (axios) for the cookie to be sent. Set `JWT_SECRET` in `.env`
(required, no default) before starting the application.

### OWASP coverage (SSDLC)

- **A01 (Broken Access Control / IDOR-BOLA):** `student-preferences` is always resolved from
  the `holderId` in the JWT (`/me`); no endpoint takes an `{id}` and the repository is
  queried by `holderId`.
- **A02 (Cryptographic Failures):** BCrypt password hashing, signed JWT (never `alg: none`).
- **A03 (Injection):** Spring Data JPA plus Bean Validation at the edge, no concatenated SQL.
- **A04 (Insecure Design):** sign-in returns a single generic error, never revealing whether the
  email exists or the password was wrong (prevents user enumeration).
- **A08 (Logging Failures):** failed sign-in attempts are logged without the password.
- **A10 (Authentication Attacks):** stateless, short-lived JWT. MFA and rate-limiting are out of
  scope for now (they require infrastructure not included here).

## API Endpoints

| Method | Path                                            | Auth                           |
| ------ | ----------------------------------------------- | ------------------------------ |
| `POST` | `/api/v1/authentication/sign-up`                | No                             |
| `POST` | `/api/v1/authentication/sign-in`                | No                             |
| `POST` | `/api/v1/authentication/sign-out`               | No                             |
| `GET`  | `/api/v1/student-preferences/me`                | Yes (holderId from JWT cookie) |
| `PUT`  | `/api/v1/student-preferences/me/daily-reminder` | Yes (holderId from JWT cookie) |
| `PUT`  | `/api/v1/student-preferences/me/dark-mode`      | Yes (holderId from JWT cookie) |
| `GET`  | `/actuator/health`                              | No                             |
| `GET`  | `/swagger-ui.html`, `/v3/api-docs`              | No                             |

## Error Handling

Business errors are answered as RFC 9457 `ProblemDetail` bodies:
`AuthenticationControllerAdvice` maps `EmailAlreadyRegisteredException` to `409` and
`InvalidCredentialsException` to `401`. Requests without a valid JWT cookie get an empty
`401` from `SecurityConfig`.

Unexpected exceptions (anything not mapped by a module's own `ControllerAdvice`) are caught by
`shared/interfaces/rest/GlobalExceptionHandler`, which returns a generic `500` body —
never the exception message or stack trace — while logging the real cause server-side.
Spring MVC's own well-known exceptions (malformed JSON, validation errors, wrong HTTP
method) keep their correct `4xx` status untouched.

## Testing

```bash
mvn test -Dtest=ArchitectureTest   # module boundaries (ArchUnit)
mvn test                           # full suite
```

The suite needs no running database: domain tests are plain JUnit, application and
persistence tests use Mockito, and controller tests use `@WebMvcTest` with the real
security chain imported. Each layer of `iam` has its own test (e.g. `UserTest`,
`StudentPreferencesCommandServiceImplTest`, `StudentPreferencesRepositoryImplTest`,
`AuthenticationControllerTest`, `DailyStudyReminderJobTest`, `IamContextFacadeImplTest`)
to copy from when adding a new module.

> If the IDE's Java extension compiles into the same `target/classes` while Maven runs
> `clean`, the MapStruct implementations can disappear and controller tests fail with a
> missing `*Assembler` bean. Re-running the build fixes it.

CI (`.github/workflows/ci.yml`) runs the full suite against an ephemeral PostgreSQL on
every push and pull request to `main`, `develop`, and `release/**` — see
[Git Workflow](#git-workflow).
