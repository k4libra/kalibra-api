# MEMORY.md - Kalibra API

Inter-session project memory. This file contains ~50 lines: summarize or remove content that no longer adds value.

## Current status (2026-10-02)
- Branch `feature/close-circuits` (from `develop`, includes the merge of `feature/KAL-38`): the four bounded contexts
  (`iam`, `curriculum`, `enrollment`, `progress`) boot, migrate and validate against PostgreSQL 17.
- All 34 user stories have their backend. The ones that need the Adaptive Engine (US-005, 006, 007, 012) are complete
  up to the ACL: `ExternalCurricularExtractionService`, `ExternalExerciseGenerationService` and
  `ExternalMasteryEstimationService` are beans with their final signatures that throw until the engine is wired.
- `curriculum` gained `GeneratedExercise` (migration `curriculum/V2`), its two controllers and three facade operations.
  `progress` is complete (migration `progress/V1`, history table `flyway_progress_exercise_attempts`).

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
- Uploads are checked against the signature of their declared format before being stored (corrupt file is `415`).

## Lessons learned and mistakes to avoid
- The VS Code Java extension rewrites `target/classes` while Maven runs: always build with `mvn clean ...`.
  A stale class shows up as `ClassNotFoundException` for a class name without package in a `@WebMvcTest`.
- Port 5432 may be taken by another project's Postgres. Run the compose under another project name and port:
  `DB_PORT=5433 docker compose -p kalibra-verify up -d`, export `DB_PORT=5433` for the app, then `down -v`.
- The JWT cookie is `Secure`: with curl, read it from `Set-Cookie` and send it back in a `Cookie` header.
- Two handlers on the same path that differ only in `produces` resolve to JSON for `Accept: */*` (media types are
  compared alphabetically); keep `produces` explicit on both.
- Data seeded by SQL bypasses the gap map eviction; a roster change or a restart refreshes it.

## Next steps (to operate the MVP)
- Wire the engine: Redis streams for extraction and generation, REST for mastery. Generation needs the subtopic
  name, which `generate(command, context)` does not receive.
- Decide the HTTP status for an engine failure (today an unmapped exception answers `500`).
- Replace `RedisGapMapCacheService` with Redis and `FileSystemMaterialStorageService` with Cloudflare R2.
- Students have no endpoint to list the courses they are enrolled in; the course id only comes back when accepting
  an invitation. Decide it in the diagram before the mobile app needs it.
- Push delivery (`PushReminderNotificationService`, `PushInvitationNotificationService`) still only logs.
