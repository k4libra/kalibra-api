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
- Multipart file upload stored in Cloudflare R2 (private bucket, presigned URLs)
- Adaptive Engine integration: REST for mastery, Redis streams for extraction and generation
- Redis cache for the mastery gap map
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
- An upload whose content is not a real PDF, PNG or JPEG (corrupt or mislabeled file) is
  rejected with `415` before anything is stored or registered.
- Keep a teacher workspace with the active course, switchable at any time.
- Generate exercises for a subtopic on the teacher's request (1 to 10), anchored to the
  `READY` material of that subtopic. A subtopic without ready material answers `409`.
- Keep **every** exercise the Adaptive Engine returns, approved or discarded, with its
  verification result and rejection reason (`GeneratedExercise`). A discarded exercise is
  only visible to the teacher: it is never delivered to a student nor has an answer key
  (`GeneratedExercise.isAvailableToStudents`).
- List the generated exercises of a course (paginated, optional `subtopicId` filter) with
  their content and verdict, and the exercise catalog: per course and subtopic, how many
  were generated, approved and discarded (courses without exercises are listed at zero).
- Exposes the `CurriculumContextFacade` (OHS) so other contexts can check course ownership,
  read course summaries and subtopics, ask for a verified practice exercise for a student,
  fetch the answer key of an approved exercise and read the verification stats per subtopic.

Ingestion is event-driven: `MaterialUploaded` is handled after commit by
`MaterialUploadedEventHandler`, and `PendingIngestionReconciliationJob` retries every
material still pending (every 5 minutes by default), so a failure after the commit is never
lost. Content extraction and exercise generation are delegated to the Adaptive Engine through
`ExternalCurricularExtractionService` and `ExternalExerciseGenerationService`. Both publish a
task on the engine's Redis stream (`kalibra:engine:tasks`) and wait for the outcome with the
same `taskId` on `kalibra:engine:results` (`shared/engine/EngineTaskClient`). A material the
engine rejects (`422`) becomes `INGESTION_ERROR`; when the engine, an AI provider or its key is
unavailable the material stays `PENDING_INGESTION` and is retried, and generation answers `503`.

Files never go to PostgreSQL: only their metadata and an opaque `storageReference` are
stored. The rest of the module only knows `MaterialStorageService`. `R2MaterialStorageService`
keeps the files in a private Cloudflare R2 bucket and hands the engine a short-lived presigned
URL; until the `R2_*` credentials are set, uploads answer `503`.
`FileSystemMaterialStorageService` (`STORAGE_PROVIDER=filesystem`) is for local development
only: the engine cannot read those files, so that material stays pending.

### Enrollment Context

The Enrollment Context owns the invitations a teacher sends to their courses and the
enrollments they turn into. It includes the following features:

- Invite a registered student to one of my courses by email. The invitation stays `PENDING`
  for 3 days and the student is notified. An email without a student account answers `422`
  and notifies nobody; a course of another teacher answers `404`.
- A student has at most one pending invitation per course and is enrolled at most once:
  inviting a student who is already invited or enrolled answers `409`.
- Cancel a pending invitation, or resend a canceled or expired one: it is `PENDING` again
  for 3 more days and the student is notified again. Resending in any other state is `409`.
- Students list their pending invitations (course, teacher email, sent and expiry dates) and
  accept or reject them. Accepting enrolls the student in the course; rejecting does not.
- `InvitationExpirationJob` runs every minute and marks as `EXPIRED` every pending
  invitation past its validity. Past its validity an invitation can no longer be answered,
  even before the job marks it.
- Teachers list their sent invitations and their enrolled students, both grouped by course.
  Every course of the teacher is listed, including those without invitations or students.
  Sent invitations can be filtered by status (`?status=PENDING`, `ACCEPTED`, `REJECTED`,
  `EXPIRED` or `CANCELED`); the filter applies inside each course, never drops a course.
- Exposes the `EnrollmentContextFacade` (OHS) so other contexts can check, in-process,
  whether a student is enrolled in a course and fetch its roster.

One transaction, one aggregate: accepting changes the `Invitation`, and
`InvitationAcceptedEventHandler` creates the `Enrollment` after the commit. If that handler
fails, `EnrollmentContextFacade.isStudentEnrolled` finds the accepted invitation and creates
the enrollment then (self-healing). Invitation notifications go through
`InvitationNotificationService`; the current implementation
(`PushInvitationNotificationService`) only logs until the mobile push provider is defined.

### Progress Context

The Progress Context (core) owns what students do with the exercises and what the teacher
learns from it. It composes `EnrollmentContextFacade` and `CurriculumContextFacade`. It
includes the following features:

- Students list the subtopics of a course they are enrolled in, each with their mastery
  level (`NO_DATA` until they answer an exercise of it).
- Students request a practice exercise for a subtopic. It is generated for their current
  mastery and only an exercise approved by the verification is delivered, with its four
  options in order A to D and without the answer.
- Students answer an exercise. The answer is graded against the answer key, the mastery of
  the subtopic is re-estimated and the response shows the result, the explanation and how
  many percentage points the mastery went up or down (or that it did not change). The first
  answer in a subtopic starts from the base mastery.
- Students read their progress (mastery and solved exercises per subtopic, feedback of the
  latest answers) and their history of solved exercises, paginated and filterable by result
  (`ALL`, `CORRECT`, `INCORRECT`) with the count of each.
- Teachers read the progress of one enrolled student, the mastery gap map of the course
  (group mastery, students per level and reinforcement priority per subtopic, plus each
  student's mastery) and the four course indicators: accuracy, practice, mastery evolution
  and verification approval rate.
- Teachers export the indicators as CSV (`Accept: text/csv`): one row per student and
  subtopic, where the student is only an anonymous code, never a name or an email. A course
  without solved exercises has nothing to export (`404`).
- A plain-language guide explains what each indicator measures and what a good signal is.

One transaction, one aggregate: submitting an answer saves the `ExerciseAttempt` with its
`MasteryChange` (so the student sees the change at once) and publishes `AnswerRecorded`;
`AnswerRecordedEventHandler` applies the estimate to `SubtopicMastery` after the commit and
drops the cached gap map of the course. If that handler fails, the next answer still starts
from the last recorded attempt (self-healing). The first estimate of each student and
subtopic is kept as the baseline of the mastery evolution.

Mastery is estimated by the Adaptive Engine through `ExternalMasteryEstimationService`, a
synchronous REST call (`POST /api/v1/mastery-estimates`) so the student sees the change right
after answering; if the engine is down, answering an exercise answers `503` and nothing is
recorded. The gap map is cached in Redis by `RedisGapMapCacheService` (10 minutes by default);
without Redis it is simply computed on every request.

Mastery values travel as percentages (0 to 100). Levels are `LOW` (below 40), `MEDIUM`
(40 to 70) and `HIGH` (above 70).

## Technology Stack

| Concern            | Technology                               |
| ------------------ | ---------------------------------------- |
| Language           | Java 25                                  |
| Framework          | Spring Boot 3.5.15                       |
| Persistence        | Spring Data JPA · PostgreSQL 17 · Flyway |
| Queue and cache    | Spring Data Redis · Redis 8              |
| File storage       | Cloudflare R2 (AWS SDK for Java v2, S3)  |
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
├── curriculum/ Supporting — courses, their material and the exercises generated from it.
│               Course (with Subtopic entities), CurricularMaterial, GeneratedExercise and
│               TeacherWorkspace aggregates; exposes CurriculumContextFacade.
├── enrollment/ Supporting — invitations and enrollments. Invitation and Enrollment
│               aggregates; consumes IamContextFacade and CurriculumContextFacade, exposes
│               EnrollmentContextFacade.
├── progress/   Core — answers, mastery and course indicators. ExerciseAttempt and
│               SubtopicMastery aggregates plus the MasteryGapAnalyzer,
│               CourseIndicatorsCalculator and StudentAnonymizer domain services; consumes
│               CurriculumContextFacade and EnrollmentContextFacade.
└── shared/     Cross-cutting configuration (Flyway per module, JWT security) and the
                inter-module contracts (shared/contracts).
```

Inter-module communication goes through in-process domain events (`UserRegistered`,
`AnswerRecorded`, ...) and three Open Host Services — `IamContextFacade` (`iam/interfaces/acl`),
`CurriculumContextFacade` (`curriculum/interfaces/acl`) and `EnrollmentContextFacade`
(`enrollment/interfaces/acl`) — whose contract types live in `shared/contracts/<module>`.

## Getting Started

### Prerequisites

- JDK 25
- Docker (PostgreSQL 17, Redis 8 and the Adaptive Engine)
- The [`kalibra-adaptive-engine`](https://github.com/k4libra/kalibra-adaptive-engine) repository cloned next to this one
- Maven 3.9+

### Configuration

```bash
cp .env.example .env
```

Everything has a default except the secrets at the top of `.env`:

| Variable                                                              | Purpose                                                      |
| --------------------------------------------------------------------- | ------------------------------------------------------------ |
| `DB_PASSWORD`, `JWT_SECRET`                                           | Own secrets (`openssl rand -base64 64` for the JWT key)      |
| `DEEPSEEK_API_KEY`, `MISTRAL_API_KEY`                                 | AI providers of the Adaptive Engine (passed to it by compose) |
| `R2_ACCOUNT_ID`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET` | Cloudflare R2 bucket for the curricular material          |

Without the provider keys and the R2 credentials the API still starts and every circuit that
does not need them works (accounts, courses, invitations, answering exercises with real mastery
estimation, progress, gap map, indicators); uploads, extraction and generation answer `503` or
stay pending until they are set.

Optional settings:

| Variable                         | Default                    | Purpose                                              |
| -------------------------------- | -------------------------- | ---------------------------------------------------- |
| `STORAGE_PROVIDER`               | `r2`                       | `filesystem` keeps files locally (development only)  |
| `MATERIALS_STORAGE_DIR`          | `./storage/materials`      | Folder of the local storage adapter                  |
| `MATERIALS_MAX_FILE_SIZE`        | `10MB`                     | Largest file accepted by the upload endpoint         |
| `R2_URL_VALIDITY`                | `PT15M`                    | Lifetime of the presigned URL handed to the engine   |
| `ENGINE_BASE_URL`                | `http://localhost:8000`    | Adaptive Engine REST endpoint (mastery estimation)   |
| `ENGINE_REQUEST_TIMEOUT`         | `PT5S`                     | Timeout of the synchronous mastery call              |
| `ENGINE_TASK_TIMEOUT`            | `PT4M`                     | How long a queued extraction or generation is awaited |
| `REDIS_URL`                      | `redis://localhost:6379/0` | Engine task queue and gap map cache                  |
| `GAP_MAP_CACHE_TTL`              | `PT10M`                    | Lifetime of a cached gap map                         |
| `INGESTION_RECONCILIATION_DELAY` | `PT5M`                     | How often pending material is retried                |

### Running the application

```bash
docker compose up --build   # this API, PostgreSQL 17, Redis 8 and the Adaptive Engine
```

To run the API from the IDE or with Maven, start only its dependencies:

```bash
docker compose up -d postgres redis engine
mvn spring-boot:run
```

The engine is built from `../kalibra-adaptive-engine` (override with `ENGINE_CONTEXT`); its
Swagger UI is at <http://localhost:8000/docs>.

Flyway creates the `iam`, `curriculum`, `enrollment` and `progress` schemas and their tables
on startup, each module with its own migrations and history table.

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

On top of that, endpoints are restricted by role (the `roles` claim of the JWT), and a valid
session with the wrong role answers `403`:

- `TEACHER` only: everything under `/api/v1/courses/**`, `/api/v1/teachers/**`,
  `/api/v1/course-invitation-groups/**`, `/api/v1/course-rosters/**` and
  `/api/v1/course-exercise-catalogs/**`, plus sending, canceling and resending invitations.
- `STUDENT` only: `PUT /api/v1/student-preferences/me/daily-reminder`, listing my pending
  invitations and accepting or rejecting them, and everything under
  `/api/v1/subtopic-masteries/**`, `/api/v1/practice-exercises/**` and
  `/api/v1/exercise-attempts/**`.
- `STUDENT` or `TEACHER`: `GET /api/v1/courses/{id}/student-progress`. Without `studentId` it
  is the student's own progress and requires being enrolled (`403` otherwise); with
  `studentId` it requires owning the course (`404` otherwise).
- Any authenticated user: reading preferences and switching dark mode.

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

- **A01 (Broken Access Control / IDOR-BOLA):** every resource is resolved against the JWT
  `holderId`. `student-preferences` and the teacher workspace are only reachable through
  `/me`; courses and their material take an `{id}`, but the repository is always queried by
  `id` + `holderId`, so a course of another teacher answers `404`, never its data. An
  invitation is resolved by `id` + teacher `holderId` (cancel, resend) or `id` + student
  (accept, reject), so someone else's invitation answers `404`. Uploaded files are stored
  under server-generated names, never a path chosen by the client. Attempts and masteries
  are always read by the student's `holderId`; a student who is not enrolled in the course
  answers `403`, an exercise that belongs to another course answers `404`, and every teacher
  view of progress first checks that the course is theirs (`404`).
- **A02 (Cryptographic Failures):** BCrypt password hashing, signed JWT (never `alg: none`).
- **A03 (Injection):** Spring Data JPA plus Bean Validation at the edge, no concatenated SQL.
- **A04 (Insecure Design):** sign-in returns a single generic error, never revealing whether the
  email exists or the password was wrong (prevents user enumeration).
- **A08 (Logging Failures):** failed sign-in attempts are logged without the password.
- **A10 (Authentication Attacks):** stateless, short-lived JWT. MFA and rate-limiting are out of
  scope for now (they require infrastructure not included here).

## API Endpoints

| Method | Path                                                    | Auth                         |
| ------ | ------------------------------------------------------- | ---------------------------- |
| `POST` | `/api/v1/authentication/sign-up`                        | No                           |
| `POST` | `/api/v1/authentication/sign-in`                        | No                           |
| `POST` | `/api/v1/authentication/sign-out`                       | No                           |
| `GET`  | `/api/v1/student-preferences/me`                        | Yes (own preferences)        |
| `PUT`  | `/api/v1/student-preferences/me/daily-reminder`         | `STUDENT` (own preferences)  |
| `PUT`  | `/api/v1/student-preferences/me/dark-mode`              | Yes (own preferences)        |
| `POST` | `/api/v1/courses`                                       | `TEACHER`                    |
| `GET`  | `/api/v1/courses`                                       | `TEACHER` (own courses only) |
| `GET`  | `/api/v1/courses/{id}`                                  | `TEACHER` (own course only)  |
| `POST` | `/api/v1/courses/{id}/curricular-materials` (multipart) | `TEACHER` (own course only)  |
| `GET`  | `/api/v1/courses/{id}/curricular-materials?page&size`   | `TEACHER` (own course only)  |
| `GET`  | `/api/v1/teachers/me/workspace`                         | `TEACHER`                    |
| `PUT`  | `/api/v1/teachers/me/workspace/active-course`           | `TEACHER` (own course only)  |
| `POST` | `/api/v1/invitations`                                   | `TEACHER` (own course only)  |
| `GET`  | `/api/v1/invitations?status=PENDING`                    | `STUDENT` (own invitations)  |
| `POST` | `/api/v1/invitations/{id}/cancellations`                | `TEACHER` (own invitation)   |
| `POST` | `/api/v1/invitations/{id}/renewals`                     | `TEACHER` (own invitation)   |
| `POST` | `/api/v1/invitations/{id}/acceptances`                  | `STUDENT` (own invitation)   |
| `POST` | `/api/v1/invitations/{id}/rejections`                   | `STUDENT` (own invitation)   |
| `GET`  | `/api/v1/course-invitation-groups?status`               | `TEACHER` (own courses only) |
| `GET`  | `/api/v1/course-rosters`                                | `TEACHER` (own courses only) |
| `POST` | `/api/v1/courses/{id}/generated-exercises`              | `TEACHER` (own course only)  |
| `GET`  | `/api/v1/courses/{id}/generated-exercises?subtopicId&page&size` | `TEACHER` (own course only) |
| `GET`  | `/api/v1/course-exercise-catalogs`                      | `TEACHER` (own courses only) |
| `GET`  | `/api/v1/subtopic-masteries?courseId`                   | `STUDENT` (enrolled)         |
| `POST` | `/api/v1/practice-exercises`                            | `STUDENT` (enrolled)         |
| `POST` | `/api/v1/exercise-attempts`                             | `STUDENT` (enrolled)         |
| `GET`  | `/api/v1/exercise-attempts?courseId&result&page&size`   | `STUDENT` (own attempts)     |
| `GET`  | `/api/v1/courses/{id}/student-progress`                 | `STUDENT` (enrolled, own)    |
| `GET`  | `/api/v1/courses/{id}/student-progress?studentId`       | `TEACHER` (own course only)  |
| `GET`  | `/api/v1/courses/{id}/mastery-gap-map`                  | `TEACHER` (own course only)  |
| `GET`  | `/api/v1/courses/{id}/indicators` (JSON, or CSV with `Accept: text/csv`) | `TEACHER` (own course only) |
| `GET`  | `/api/v1/courses/{id}/indicators/guide`                 | `TEACHER`                    |
| `GET`  | `/actuator/health`                                      | No                           |
| `GET`  | `/swagger-ui.html`, `/v3/api-docs`                      | No                           |

## Error Handling

Business errors are answered as RFC 9457 `ProblemDetail` bodies:
`AuthenticationControllerAdvice` maps `EmailAlreadyRegisteredException` to `409` and
`InvalidCredentialsException` to `401`. `SecurityConfig` answers an empty `401` to requests
without a valid JWT cookie and an empty `403` to a valid session with the wrong role.

Curriculum errors follow the same format: a course without subtopics is `422`, a course
that does not exist or belongs to another teacher is `404`, an unsupported material format
(or a corrupt file) is `415`, a subtopic outside the course or an empty file is `400`, and a file above the size
limit is `413`.

Enrollment errors too: an email without a student account is `422`; a course or an
invitation of someone else is `404`; answering or canceling an invitation that is no longer
pending (or past its validity), resending one that is neither canceled nor expired, and
inviting a student already invited or enrolled are `409`; a `status` filter other than
`PENDING` on `/invitations`, or other than an invitation status on
`/course-invitation-groups`, is `400`.

Generated exercises: a subtopic without ready material is `409`; a quantity outside 1..10 or
a subtopic outside the course is `400`.

Progress errors: a student who is not enrolled in the course is `403`; an exercise that does
not exist, was discarded or belongs to another course, and a subtopic with no verified
exercise to deliver, are `404`; an option other than A to D or an unsupported `result` filter
is `400`; a course of another teacher is `404`; exporting a course without solved exercises
is `404`.

When the Adaptive Engine, one of its AI providers or the material storage cannot answer
(down, timed out or not configured yet), the request is valid and may be retried: it is `503`
with a generic detail, never `500`.

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
