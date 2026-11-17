# MEMORY.md - Kalibra API

## Current status (2026-11-16)
- `feature/remove-redis` (from develop, uncommitted): Redis removed. Extraction/generation use `EngineTaskClient` (`ENGINE_TASK_TIMEOUT`); gap maps use `InMemoryGapMapCacheService`. Prior live web/mobile E2E passed without Redis.
- Exercise reuse pool + difficulty fallback implemented: answered IDs pass through progress/OHS, random approved pool lookup, explicit engine target, HARD -> MEDIUM and async refill; curriculum V5 adds the pool index. `mvn clean verify`: 597 tests pass (5 ArchUnit), no DB. Existing work preserved; no staging, commit, push or branch switch.
- Material upload gate complete: teacher GET `/api/v1/curricular-extraction-availability`; unavailable uploads fail before storage with 503 `MATERIAL_UPLOAD_UNAVAILABLE`. Seven real HTTP checks passed in an isolated web context (teacher true/false, anonymous 401, three roles 403, upload 503); engine/web changes remain outside this API task.
- 2026-11-11 QA: initial 69 flows documented; correction reran 46 desktop flows against another isolated local-DB copy and refreshed the shared native invitation. Originals/source preserved; BKT/native evidence retained. Mistral/expiry pending; push adapter only logs. All correction services/data/configs removed; owned R2 PDF deleted (204). Evidence: `../../../references/functional tests`.
- `feature/course-subtopic-editing`: course PATCH; subtopic POST/PATCH/DELETE; ACTIVE/ARCHIVED status (curriculum V3).
- Teacher resources include all subtopics with status/locked. Any material locks rename/removal; locks use one query per course.
- Archived subtopics reject upload/generation (409) and leave student lists/practice, gap maps and indicator subtopic lines.
- Preferences now use only PATCH `/student-preferences/me`; obsolete PUT commands/resources are removed.
- Shared contract complete: course metadata, subtopic descriptions/objects (legacy names accepted), material metadata/20MB, express practice, invitation expiry filters and structured attempt feedback. Curriculum V4 and progress V2 added.
- Prior validation: 29 HTTP contract checks passed on isolated PostgreSQL 17 + fixture engine, including JSONB/upload boundaries and Flyway V4/V2/JPA startup; temporary infrastructure removed. V5 was not applied to a live DB in this task.
- `docker compose up --build` starts API + engine + PostgreSQL; mastery/extraction/generation use REST, materials use R2; external providers were not rerun here.

## Decisions (and why)
- curriculum V6 and progress V3 strip the same source lead-in from stored statements and attempt snapshots (engine rule mirrored in SQL); only the leading clause changes.
- Upload availability uses the short engine client and a synchronized, clock-testable per-instance cache (`ENGINE_CAPABILITIES_TTL`, default PT30S). Failures cache false, never stale true, so recovery needs no API restart. Engine task errors retain status but discard external detail to keep messages/logs generic.
- Difficulty mirrors engine bands: null/<0.40 EASY, <=0.70 MEDIUM, otherwise HARD. Pool ignores origin, requires both verification checks, and excludes the holder's answered IDs; remaining stock also excludes the delivered exercise. Only empty HARD results retry MEDIUM (pool first); outages still propagate.
- Refill claims a per-instance course/subtopic/difficulty key before scheduling, releasing it on completion/failure/rejection. Separate coordinator and @Async worker ensure proxy interception; 2 threads + queue 32 bound engine load. All refill failures only log; engine calls remain outside DB transactions.
- EXPRESS practice tries the next rotation candidate when a subtopic yields no verified exercise (max 2 tries) so a 5-exercise session does not stop; observed: basic material + HARD level often discards every candidate.
- Course owns editing invariants: at least one ACTIVE subtopic, contiguous unique order and validation before mutation. Application resolves material existence by course/subtopic and passes locked to the pure aggregate; ownership failures are 404.
- Curriculum `fetchSubtopics` exposes ACTIVE only; `fetchHistoricalSubtopics` retains archived names for historical consumers. Gap-map reads self-heal when the roster or active subtopic names change, avoiding stale archived/renamed lines.
- StudentPreferences validates reminder enabled-without-time (400); PATCH retains omitted fields/time. Times retain minute precision; HH:mm and HH:mm:ss UTC inputs are accepted.
- Edited module advices have explicit higher precedence than the global fallback, preserving contract ProblemDetail codes.
- Every engine attempt is persisted atomically with saveAll; only isAvailableToStudents() exercises leave curriculum, including answer keys.
- Generated exercises and answer attempts persist steps/key idea; attempts snapshot option texts/correct key/time. Legacy explanations derive TEXT/CODE steps at read time.
- Express ranks ACTIVE/READY subtopics by <40%, no data, then remaining mastery; per-instance student/course cursors rotate and skip the latest persisted attempt when possible.
- MasteryEstimate adds prior and initializedFromBase to the diagram's probability/level, exposing the first change from the base.
- GeneratedExercise.fromEngine keeps the diagram signature; withContent supplies content absent from that signature.
- AnswerRecordedEventHandler evicts the gap cache after mastery commits (MasteryUpdated has no course id).
- Answer engine calls stay outside transactions; TransactionTemplate wraps save/publish.
- Mastery resources use percentages 0..100; domain MasteryProbability stays 0..1.
- ADMINISTRATOR is web-only/read-only and has no TEACHER/STUDENT roles; analytics contain no student identity.
- Unknown paths denyAll; docs and seeder are opt-in. Upload format signatures are verified before storage (corrupt -> 415).

## Pitfalls
- VS Code Java extension can rewrite target/classes during Maven: use clean and retry missing MapStruct beans.
- Avoid occupied ports; use isolated temporary infrastructure and remove it after verification. Multipart requests reserve 1MB beyond the configured file limit for framing.
- Engine RestClient uses HTTP/1.1: uvicorn rejects the JDK client's h2c upgrade.
- Secure JWT cookies need manual Cookie forwarding for local HTTP verification.
- Identically named consumer ACL services need explicit per-context bean names.
- Same-path JSON/CSV handlers need explicit produces; Accept */* resolves to JSON.
- SQL seeding bypasses cache eviction; read validation refreshes changed rosters/subtopics.

## Pending
- Live upload/extraction/generation still depend on engine/R2 setup. Empty `MISTRAL_API_KEY` disables uploads; set it and restart the engine to re-enable them after the capabilities TTL.
- Evolution baseline is the first posterior, not the engine prior; decide whether to change it.
- Update diagrams for user/student-course/administrator endpoints and course/subtopic editing + preferences PATCH.
- Push notification delivery only logs until a provider is selected.
- Gap-map cache and refill deduplication are per instance; several API instances may refill the same pool concurrently.
