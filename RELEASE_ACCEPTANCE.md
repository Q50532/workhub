# WorkHub V2 integrated release candidate — acceptance checklist (2026-10-27)

This integrates the **existing WorkHub V2 Plus** with three follow-up changes:
- Audited `CREATE`, `COMMENT`, `RESOLUTION_MARK`, and `RESOLUTION_UNMARK` (inside transactions). Previous audit for `EDIT`, `STATUS`, `ASSIGNEE`, `PRIORITY`, `DELETE` retained.
- MANAGER-only `GET /api/notifications/failed` and `POST /api/notifications/{id}/retry`; retries use the durable task queue and existing bounded executor.
- `verify_final_readonly.ps1` read-only live HTTP/ACL smoke test and `FinalCoverageTest.java` regression tests.

## What is present

USER/MANAGER role authorization; object-level Ticket visibility; Ticket workflow, comments, resolution; status logs and actor audit; `If-Match` version precondition + 409 conflict; Redis Cache Aside and single-instance miss suppression; Lua rate limiting with HTTP 429; MySQL idempotency keys on Ticket creation; SLA overdue notification; MySQL notification outbox + bounded Java Thread Pool; scheduled recovery + manager retry; pagination, Dynamic SQL, frontend, SQL analysis & bounded load tools.

## Setup on an existing **Phase 1** MySQL database

**Do not overwrite the working Phase 1 folder. Back up the workhub database before applying any SQL.**

Apply these *additive* migrations **in order** using your normal MySQL client:

1. `sql/v2_completion_migration.sql`: audit + durable notifications.
2. `sql/v2_plus_migration.sql`: idempotency + SLA and one-time deadline backfill.

These scripts use `CREATE TABLE IF NOT EXISTS`, but do not automatically repair previously partially created tables. Review schema on any prior migration error. Do **not** rerun `sql/v2_migration.sql` on an already migrated Phase 1 DB. `sql/v2_fresh_schema.sql` is for blank databases only.

The application uses `${DB_PASSWORD}` from Windows environment; it must contain the correct MySQL credential. Redis runs at localhost:6379. Java 25 is required by the POM.

```powershell
cd D:\Desktop\Internship\workhub_v2_integrated_candidate\workhub
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Second terminal, choose **an existing private Ticket created by Tom and NOT assigned to Alice** (e.g. #16 only if unchanged):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\verify_final_readonly.ps1 -TicketId 16
```

* Optional mutating tests on a disposable database / test Tickets only:

```powershell
py .\bench\verify_idempotency.py
py .\tests_concurrency.py
```

* Optional read-only load sample (no benchmark number has been invented):

```powershell
py .\bench\read_only_load.py --requests 200 --concurrency 10 --user Tom
```

## Testing status

**Successfully performed in this build sandbox:** Java 21 compiled and ran isolated `RateLimitRule`, `SlaPolicy` and model classes; a small logic harness passed. `node --check` passed on both frontend JS modules. XML parsing of `pom.xml` passed. Source and ZIP checks passed.

**NOT performed / NOT claimed:** full Maven project compile/test (Maven wrapper download blocked; sandbox has Java 21, target is Java 25), live MySQL/Redis integration, HTTP permissions, 200/409 concurrency race, real overload and JMeter/k6-scale performance. The previous Phase 1 (21/21 HTTP, 17/17 tests) is a **different build** and does not certify this candidate.

## Known design boundaries

- Redis cache stampede suppression is intra-JVM only. This is **not** multi-node distributed locking.
- The TTL / eventual cache consistency design does not claim linearizability.
- SLA breach triggers notifications, not unauthorized Ticket reassignment or automatic escalation to another status.
- The fixed-window Redis limiter applies to specified write routes, not every request.
- `Idempotency-Key` is optional for older clients; only keyed requests are deduplicated.
- No external email service or distributed message broker; local durable notifications are delivered through MySQL outbox and Thread Pool.
- A production deployment should add database migrations under a versioned tool, secrets management and operational monitoring.
