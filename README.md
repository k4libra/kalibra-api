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
- Scheduled jobs (Spring `@Scheduled`)
- Multipart file upload behind a pluggable storage service
- ArchUnit boundary enforcement
- Health endpoint (Spring Boot Actuator)
- Global fallback exception handler
- Continuous Integration (GitHub Actions)
- Git Flow branching strategy (`main` protected, `develop`, `feature/*`, `release/*`)

## Bounded Contexts

The application is divided into internal modules, each one a bounded context with
its own domain, application, infrastructure, and interfaces layers.

### Identity and Access Management (IAM) Context

The IAM Context is responsible for user registration and authentication. It includes
the following features:

- Register a new user (sign-up) with a securely hashed password.
- Authenticate a user (sign-in) and issue a locally signed JWT.
- Enforce unique email addresses across accounts.
- Role-based access control (RBAC) with accumulable roles (`REGISTERED_USER`,
  `ADMINISTRATOR`). Roles are additive, not exclusive — an account can hold both at
  once. Every new account gets `REGISTERED_USER` by default; granting `ADMINISTRATOR`
  never removes it. Roles travel in the JWT `roles` claim and land as
  `ROLE_<name>` authorities via `JwtAuthenticationFilter` — protect an endpoint with
  `.hasAuthority("ROLE_ADMINISTRATOR")` in `SecurityConfig` (see the example comment
  there). Add new roles by extending the `Role` enum; there is no built-in endpoint to
  grant a role — that flow is decided by the Kalibra product requirements as the
  application evolves.

On successful registration it publishes the `UserRegistered` domain event, allowing
other contexts to react in-process while IAM stays decoupled from them.

### Curriculum Context

The Curriculum Context owns the courses a teacher creates and the material they upload
for them. It includes the following features:

- Create a course with its subtopics (at least one, kept in display order). A teacher only
  ever sees the courses they created: listing is scoped to the JWT `holderId`, and asking
  for another teacher's course answers `404`.
- Upload curricular material (PDF, PNG or JPEG) anchored to one or more subtopics of the
  course. It starts as `PENDING_INGESTION` and turns `READY` (usable to generate exercises)
  or `INGESTION_ERROR` (with its `failureReason`).
- List the material of a course, paginated and newest first, with the ingestion status of
  each file.
- Keep a teacher workspace with the active course, switchable at any time.
- Exposes the `CurriculumContextFacade` (OHS) so other contexts can check course ownership
  and read course summaries and subtopics in-process.

Ingestion is event-driven: `MaterialUploaded` is handled after commit by
`MaterialUploadedEventHandler`, and `PendingIngestionReconciliationJob` retries every
material still pending (every 5 minutes by default), so a failure after the commit is never
lost. Content extraction is delegated to the Adaptive Engine through
`ExternalCurricularExtractionService`; **that engine is not connected yet**, so material stays
`PENDING_INGESTION` until it is.

Files never go to PostgreSQL: only their metadata and an opaque `storageReference` are
stored. The rest of the module only knows `MaterialStorageService`; the storage provider is
still to be decided, and `FileSystemMaterialStorageService` is a provisional local adapter
(`MATERIALS_STORAGE_DIR`). Moving to S3, Azure Blob or similar means adding another
implementation of that interface.

Students will see the subtopics of the courses they were invited to once the enrollment
context exists; it will consume `CurriculumContextFacade`.

## Technology Stack

| Concern            | Technology                               |
| ------------------ | ---------------------------------------- |
| Language           | Java 25                                  |
| Framework          | Spring Boot 3.5.15                       |
| Persistence        | Spring Data JPA · PostgreSQL 17 · Flyway |
| Mapping            | MapStruct 1.6.3                          |
| Security           | Spring Security · JWT (jjwt 0.12.6)      |
| Architecture tests | ArchUnit 1.4.1                           |

Lombok, Flyway, and the PostgreSQL driver are managed by the `spring-boot-starter-parent` BOM.

## Project Structure

```
com.kalibra.api
├── iam/        Core — authentication. User aggregate (Email + HashedPassword VOs),
│               issues its own JWT and publishes the UserRegistered event.
├── curriculum/ Supporting — courses and their material. Course (with Subtopic entities),
│               CurricularMaterial and TeacherWorkspace aggregates; exposes
│               CurriculumContextFacade.
└── shared/     Cross-cutting configuration (Flyway per module, JWT security) and the
                inter-module contracts (shared/contracts).
```

Inter-module communication goes through in-process domain events (`UserRegistered`) and
the `CurriculumContextFacade` Open Host Service (`curriculum/interfaces/acl`), whose
contract types live in `shared/contracts/curriculum`.

## Getting Started

### Prerequisites

- JDK 25
- Docker (PostgreSQL 17)
- Maven 3.9+

### Configuration

```bash
cp .env.example .env   # set DB_PASSWORD and JWT_SECRET (openssl rand -base64 64)
```

Curricular material settings (all optional):

| Variable                         | Default               | Purpose                                         |
| -------------------------------- | --------------------- | ----------------------------------------------- |
| `MATERIALS_STORAGE_DIR`          | `./storage/materials` | Folder of the provisional local storage adapter |
| `MATERIALS_MAX_FILE_SIZE`        | `10MB`                | Largest file accepted by the upload endpoint    |
| `INGESTION_RECONCILIATION_DELAY` | `PT5M`                | How often pending material is retried           |

### Running the application

```bash
docker compose up -d   # starts PostgreSQL 17 only
mvn spring-boot:run    # or run the application from the IDE
```

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
stays public (sign-up/sign-in/sign-out); every other endpoint requires a valid JWT.

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

- **A01 (Broken Access Control / IDOR-BOLA):** courses, their material and the teacher
  workspace are resolved against the JWT `holderId`; a course of another teacher answers
  `404`, never its data. Uploaded files are stored under server-generated names, never a
  path chosen by the client.

- **A02 (Cryptographic Failures):** BCrypt password hashing, signed JWT (never `alg: none`).
- **A03 (Injection):** Spring Data JPA plus Bean Validation at the edge, no concatenated SQL.
- **A04 (Insecure Design):** sign-in returns a single generic error, never revealing whether the
  email exists or the password was wrong (prevents user enumeration).
- **A08 (Logging Failures):** failed sign-in attempts are logged without the password.
- **A10 (Authentication Attacks):** stateless, short-lived JWT. MFA and rate-limiting are out of
  scope for now (they require infrastructure not included here).

## API Endpoints

| Method | Path                                                    | Auth                   |
| ------ | ------------------------------------------------------- | ---------------------- |
| `POST` | `/api/v1/authentication/sign-up`                        | No                     |
| `POST` | `/api/v1/authentication/sign-in`                        | No                     |
| `POST` | `/api/v1/authentication/sign-out`                       | No                     |
| `POST` | `/api/v1/courses`                                       | Yes                    |
| `GET`  | `/api/v1/courses`                                       | Yes (own courses only) |
| `GET`  | `/api/v1/courses/{id}`                                  | Yes (own course only)  |
| `POST` | `/api/v1/courses/{id}/curricular-materials` (multipart) | Yes (own course only)  |
| `GET`  | `/api/v1/courses/{id}/curricular-materials?page&size`   | Yes (own course only)  |
| `GET`  | `/api/v1/teachers/me/workspace`                         | Yes                    |
| `PUT`  | `/api/v1/teachers/me/workspace/active-course`           | Yes (own course only)  |
| `GET`  | `/actuator/health`                                      | No                     |

## Error Handling

Curriculum errors are answered as RFC 9457 `ProblemDetail` bodies: a course without
subtopics is `422`, a course that does not exist or belongs to another teacher is `404`, an
unsupported material format is `415`, a subtopic outside the course or an empty file is
`400`, and a file above the size limit is `413`.

Unexpected exceptions (anything not mapped by a module's own `ControllerAdvice`, e.g.
`AuthenticationControllerAdvice`) are caught by
`shared/interfaces/rest/GlobalExceptionHandler`, which returns a generic `500` body —
never the exception message or stack trace — while logging the real cause server-side.
Spring MVC's own well-known exceptions (malformed JSON, validation errors, wrong HTTP
method) keep their correct `4xx` status untouched.

## Testing

```bash
mvn test -Dtest=ArchitectureTest   # module boundaries (ArchUnit) — no Postgres needed
mvn test                           # full suite — requires Postgres (docker compose up -d)
```

CI (`.github/workflows/ci.yml`) runs the full suite against an ephemeral PostgreSQL on
every push and pull request to `main`, `develop`, and `release/**` — see
[Git Workflow](#git-workflow).
