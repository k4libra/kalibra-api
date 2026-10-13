# MEMORY.md - Kalibra API

## Current status (2026-10-12)
- All four contexts (`iam`, `curriculum`, `enrollment`, `progress`) run on PostgreSQL 17; all 34 stories have their backend.
- `feature/course-subtopic-editing`: course PATCH; subtopic POST/PATCH/DELETE; ACTIVE/ARCHIVED status (curriculum V3).
- Teacher resources include all subtopics with status/locked. Any material locks rename/removal; locks use one query per course.
- Archived subtopics reject upload/generation (409) and leave student lists/practice, gap maps and indicator subtopic lines.
- Preferences now use only PATCH `/student-preferences/me`; obsolete PUT commands/resources are removed.
- `mvn clean verify`: 502 tests pass (including 5 ArchUnit checks); 44 real-cookie HTTP checks passed on temporary PostgreSQL 17.
- Real startup validated Flyway V3, JPA and lock queries. Temporary runtime/database were removed; no commit or push.
- Adaptive Engine: mastery via REST, extraction/generation via Redis streams, materials via R2, gap-map cache via Redis.
- `docker compose up --build` starts API + engine + Redis + PostgreSQL; external AI/storage providers were not rerun here.
- Student progress includes max answeredAt as lastActivityAt (null without answers); practice includes stored difficulty.
- KAL-39: IAM names (V4), GET `/users/me`, GET `/students/me/courses`, web-only sign-in roles and administrator analytics.

## Decisions (and why)
- Course owns editing invariants: at least one ACTIVE subtopic, contiguous unique order and validation before any mutation.
- Application resolves material existence by course/subtopic and passes locked to the pure aggregate; ownership failures are 404.
- Curriculum `fetchSubtopics` exposes ACTIVE only; `fetchHistoricalSubtopics` retains archived names for historical consumers.
- Gap-map reads self-heal when the roster or active subtopic names change, avoiding stale archived/renamed lines.
- Reminder enabled-without-time validation belongs to StudentPreferences; PATCH retains omitted fields and stored reminder time.
- Reminder times retain minute precision; HH:mm and HH:mm:ss UTC inputs are accepted. A missing effective time answers 400.
- Edited module advices have explicit higher precedence than the global fallback, preserving contract ProblemDetail codes.
- Every engine attempt is persisted; only isAvailableToStudents() exercises leave curriculum, including answer keys.
- MasteryEstimate adds prior and initializedFromBase to the diagram's probability/level, exposing the first change from the base.
- GeneratedExercise.fromEngine keeps the diagram signature; withContent supplies content absent from that signature.
- Domain services compute only their context's data; query services complete other-context names, roster and verification stats.
- AnswerRecordedEventHandler evicts the gap cache after mastery commits (MasteryUpdated has no course id).
- Answer engine calls stay outside transactions; TransactionTemplate wraps save/publish. Generation uses atomic saveAll.
- Mastery resources use percentages 0..100; domain MasteryProbability stays 0..1.
- ADMINISTRATOR is web-only/read-only and has no TEACHER/STUDENT roles; analytics contain no student identity.
- Unknown paths denyAll; docs and seeder are opt-in. Upload format signatures are verified before storage (corrupt -> 415).

## Pitfalls
- VS Code Java extension can rewrite target/classes during Maven: use clean and retry missing MapStruct beans.
- Avoid occupied ports; use isolated temporary infrastructure and remove it after verification.
- Engine RestClient uses HTTP/1.1: uvicorn rejects the JDK client's h2c upgrade.
- Redis blocking reads must be shorter than command timeout (API poll 1 s vs 5 s).
- Secure JWT cookies need manual Cookie forwarding for local HTTP verification.
- Identically named consumer ACL services need explicit per-context bean names.
- Same-path JSON/CSV handlers need explicit produces; Accept */* resolves to JSON.
- SQL seeding bypasses cache eviction; read validation refreshes changed rosters/subtopics.

## Pending
- Provider/R2 credentials remain necessary for live upload, extraction and generation.
- Evolution baseline is the first posterior, not the engine prior; decide whether to change it.
- Update diagrams for user/student-course/administrator endpoints and course/subtopic editing + preferences PATCH.
- Push notification delivery only logs until a provider is selected.
- EngineTaskClient assumes a single API instance.
