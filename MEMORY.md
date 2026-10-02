# MEMORY.md - Kalibra API

Inter-session project memory. This file contains ~50 lines: summarize or remove content that no longer adds value.

## Current status (2026-10-02)
- The four bounded contexts (`iam`, `curriculum`, `enrollment`, `progress`) boot, migrate and validate against
  PostgreSQL 17. All 34 user stories have their backend.
- The Adaptive Engine is wired: mastery over REST, extraction and generation over Redis streams
  (`shared/engine/EngineTaskClient`), material in Cloudflare R2, gap map cache in Redis. `docker compose up --build`
  starts API + engine + Redis + PostgreSQL.
- Verified end to end without credentials: every circuit over HTTP, real BKT mastery through the engine, and the
  queue round trip (task published, consumed, `502` back, material kept pending). Real extraction and generation
  were NOT exercised: they need `DEEPSEEK_API_KEY`, `MISTRAL_API_KEY` and the `R2_*` credentials.

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
- Port 5432 may be taken by another project's Postgres. Run the compose under another project name and ports
  (`-p kalibra-verify` with `DB_PORT`, `REDIS_PORT`, `ENGINE_PORT`, `SERVER_PORT`), then `down -v`.
- The JDK HTTP client tries an h2c upgrade that uvicorn rejects: the engine `RestClient` is pinned to HTTP/1.1.
- A blocking stream read must be shorter than the Redis command timeout, on both sides (API poll 1 s vs 5 s).
- The JWT cookie is `Secure`: with curl, read it from `Set-Cookie` and send it back in a `Cookie` header.
- Two handlers on the same path that differ only in `produces` resolve to JSON for `Accept: */*` (media types are
  compared alphabetically); keep `produces` explicit on both.
- Data seeded by SQL bypasses the gap map eviction; a roster change or a restart refreshes it.

## Next steps (to operate the MVP)
- Set the provider keys and the R2 credentials in `.env`, then run a real upload → extraction → generation once
  and check the first Mistral/DeepSeek/R2 responses (never exercised against the live services).
- The evolution baseline is the first posterior, not the base prior the engine reports; decide if it should be the prior.
- Students have no endpoint to list the courses they are enrolled in; the course id only comes back when accepting
  an invitation. Decide it in the diagram before the mobile app needs it.
- Push delivery (`PushReminderNotificationService`, `PushInvitationNotificationService`) still only logs.
- `EngineTaskClient` assumes one API instance (results are acknowledged by whoever reads them).
