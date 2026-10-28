# WorkHub V2 Plus — 2026-10-27 (candidate, NOT locally accepted)

This is an **additive version** built from `workhub_v2_with_async_notifications_20261027.zip`.
It includes source, frontend, migration SQL and read-only / mutating test scripts.

## Implemented additions

1. **Redis + Lua rate limiting** via Spring MVC interceptor (user-scoped, atomic `INCR/EXPIRE`):
   - `POST /tickets`: 10/minute per authenticated user
   - `POST /tickets/{id}/comments`: 30/minute
   - `POST /api/ai/**`: 15/minute (future integration route)
   - over limit: 429 + `Retry-After`
   - Redis failure: 503 (fail closed for protected writes)
   - GET endpoints unaffected
2. **Idempotent Ticket creation** via MySQL unique `(actor_id, request_key)` and `SELECT ... FOR UPDATE`:
   - Send `Idempotency-Key: <8..128 chars>` to `POST /tickets`.
   - Same user + same key + same payload -> same Ticket ID without duplicate insertion.
   - Same key + different payload -> 409.
   - Header remains optional for backward compatibility; without it **no deduplication guarantee**.
   - Frontend generates/reuses the key until create succeeds.
   - With header, response body is **created Ticket ID**; without header legacy body remains affected-row-count `1`.
3. **SLA timeout escalation** with scheduled MySQL scan + transactionally claimed overdue row:
   - Default example targets: URGENT 60m, HIGH 240m, MEDIUM 1440m, LOW 4320m.
   - Deadline starts on creation (all new Tickets start at DB default MEDIUM) and resets on Manager priority change.
   - Overdue OPEN/IN_PROGRESS Tickets are marked once and Managers receive notifications via the existing durable queue.
   - `GET /tickets/{id}/sla` returns due time and escalated time, with row-level permission checks.
   - Scans every 60 s; notification delivery is eventually consistent.
4. **Read-only benchmark** `bench/read_only_load.py` with status counts, throughput, P50/P95/P99 (no fabricated results).
5. **SQL analysis** `sql/perf_explain.sql` with `EXPLAIN ANALYZE` queries; indexes should be changed only after observing real execution plans.
6. Extra JUnit tests for idempotency, SLA policy, notification fan-out, rate-rule matching.
7. `/error` readable by anonymous users to avoid masking original failure as HTTP 401; HTTP stack traces disabled.

## Setup for user's currently running **phase1** project

Do not overwrite the tested Phase 1 directory. Unzip to a separate folder, back up MySQL, then apply migrations in this order:

1. `sql/v2_completion_migration.sql` (audit + notification tables; `CREATE TABLE IF NOT EXISTS`).
2. `sql/v2_plus_migration.sql` (idempotency + SLA + backfill; repeat safe for rows).

The original initial migration `sql/v2_migration.sql` is **already applied** in Phase 1; do **not** run it twice. Keep the existing `DB_PASSWORD` environment variable; it is **not** renamed.

Ensure MySQL and Redis are running, then in the new project folder:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

After build succeeds, smoke-test existing Tom/Alice logins and permissions before trying new features.

**Mutating integration test** (creates a new Ticket; use disposable data only):

```powershell
py .\bench\verify_idempotency.py
```

**Read-only concurrent performance sampling** (uses existing Tickets):

```powershell
py .\bench\read_only_load.py --requests 200 --concurrency 10 --user Tom
```

For SLA, create a test Ticket, inspect `GET /tickets/{id}/sla`, change priority as Manager, and verify overdue Manager notification. Do not change system clock or mutate an existing business Ticket for the test.

## Deliberate limitations

- No live MySQL / Redis or Java 25 build was possible in this sandbox; code is **not fully acceptance-tested**. Original Phase 1's 21 HTTP and 17 JUnit PASS apply to **the old package only**.
- No actual QPS/P95 values are asserted, since user's hardware and real workload were unavailable.
- Limiter uses fixed one-minute windows, not token bucket; accepted tradeoff for modest work-ticket traffic.
- SLA is a **notification escalation**, not an automatic unauthorized Ticket status/assignee update.
- Database idempotency keys are retained indefinitely (for archival uniqueness); add an explicit retention policy only after defining business semantics.
- Cross-JVM Redis cache stampede prevention remains not distributed; rate limiting itself uses atomic Redis Lua and works across app replicas.
- The original AI Assistant `ai-assistant.zip` source was not available in this chat; the AI extension is delivered separately, not merged into the original app.
