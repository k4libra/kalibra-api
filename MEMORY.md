# MEMORY.md - Kalibra API

## Current status (2026-10-10)
- The four bounded contexts (`iam`, `curriculum`, `enrollment`, `progress`) run on PostgreSQL 17; all 34 user stories have their backend.
- The Adaptive Engine is wired: mastery over REST, extraction and generation over Redis streams
  (`shared/engine/EngineTaskClient`), material in Cloudflare R2, gap map cache in Redis. `docker compose up --build`
  starts API + engine + Redis + PostgreSQL.
- Earlier validation covered HTTP, real BKT and the queue outage round trip without provider credentials.
- This fidelity task follows a user-reported real DeepSeek/BKT E2E run; external providers were not rerun here.
- Student progress exposes max course-attempt `answeredAt` as `lastActivityAt` (null without answers). Practice
  exposes stored EASY/MEDIUM/HARD difficulty across the neutral curriculum contract, without correct answers.
- `mvn clean verify`: 426 tests pass, including ArchUnit, progress dates, practice JSON and ACL difficulty.
- KAL-39: users have first/last names (iam V4), `GET /users/me`, `GET /students/me/courses`, ADMINISTRATOR
  analytics (`/institutional-indicators`), sign-in restricted per client application, generic users seeder.

## Decisions (and why)
- Every engine attempt is persisted, approved or discarded; only `isAvailableToStudents()` exercises leave curriculum
  (practice exercise and answer key). This is the hard rule behind "0 % rejected exercises exposed".
- `MasteryEstimate` carries the engine's `prior` and `initializedFromBase` besides probability and level, so the first
  answer of a subtopic shows a real change from the base. The diagram only has the first two fields.
- `GeneratedExercise.fromEngine(command, verification)` keeps the diagram signature; the content is set with
  `withContent(...)` because that signature has nowhere to carry it.
- Domain services only compute over progress data. Names, emails, students without activity, subtopics without
  practice and verification stats are completed in `SubtopicMasteryQueryServiceImpl`.
- The gap map cache is evicted in `AnswerRecordedEventHandler` after the mastery update commits (`MasteryUpdated` has
  no course id), and a cached map is only served while it covers the current roster.
- Submitting an answer runs the engine call outside the transaction (`TransactionTemplate` only around save + publish).
  Generation is not transactional for the same reason; `saveAll` is atomic on its own.
- Mastery values travel as percentages 0..100 in resources; `MasteryProbability` stays 0..1 in the domain.
- ADMINISTRATOR is a decision taken outside the diagram (2026-10-10): web only, read-only platform-wide
  aggregates, no student identity; it never holds TEACHER/STUDENT, so every other endpoint answers 403.
- Unknown paths are `denyAll`; API docs and the seeder are opt-in env flags, never on by default.
- Uploads are checked against the signature of their declared format before being stored (corrupt file is `415`).

## Lessons learned and mistakes to avoid
- The VS Code Java extension rewrites `target/classes` during Maven: use `mvn clean ...`; retry missing MapStruct beans.
- Avoid occupied ports: use compose `-p kalibra-verify` with alternate DB/Redis/engine/server ports, then `down -v`.
- The JDK HTTP client tries an h2c upgrade that uvicorn rejects: the engine `RestClient` is pinned to HTTP/1.1.
- A blocking stream read must be shorter than the Redis command timeout, on both sides (API poll 1 s vs 5 s).
- The JWT cookie is `Secure`: with curl, read it from `Set-Cookie` and send it back in a `Cookie` header.
- Two handlers on the same path that differ only in `produces` resolve to JSON for `Accept: */*` (media types are
  compared alphabetically); keep `produces` explicit on both.
- Data seeded by SQL bypasses the gap map eviction; a roster change or a restart refreshes it.

## Next steps (to operate the MVP)
- Provider keys and R2 credentials remain necessary for live upload, extraction and generation.
- The evolution baseline is the first posterior, not the base prior the engine reports; decide if it should be the prior.
- Add `/students/me/courses`, `/users/me` and the institutional indicators to the PlantUML diagrams.
- Push delivery (`PushReminderNotificationService`, `PushInvitationNotificationService`) still only logs.
- `EngineTaskClient` assumes one API instance (results are acknowledged by whoever reads them).
