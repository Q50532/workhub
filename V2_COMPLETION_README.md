# WorkHub V2 — completion increment (source delivery)

This ZIP extends the previously validated `workhub_v2_phase1` source. It is **not** a claimed production release; tests requiring Java 25, MySQL and Redis must be run in the user's Windows environment.

## New source changes

1. Client version precondition. Every HTTP Ticket mutation now requires `If-Match: <version>` (integer, optional surrounding quotes). Missing => HTTP 428; stale => HTTP 409; the version comparison is enforced by MySQL `UPDATE ... WHERE version=?`, not only by an in-memory comparison.
2. Status, Manager assignee changes, Manager priority changes, edits (before/after fields) and deletion persist an actor-attributed audit record in the **same transaction**. Existing `ticket_logs` remains the status-only history.
3. Ticket cache misses are single-flight **inside one application JVM** using fixed lock stripes; Redis unavailability falls back to MySQL. Cache invalidation executes after DB commit using the same per-stripe lock. Does NOT guarantee multi-instance distributed cache consistency. No Redis cluster / distributed lock is claimed.
4. Browser's Ticket mutations send the `version` currently shown to the user. After HTTP 409, reload the Ticket manually; never silently retry updates with a newly fetched version.
5. Suppress Java stack traces in HTTP error responses; preserve server-side diagnostics.
6. Added unit tests for stale client version, failed concurrent update and actor audit.

## Upgrade an EXISTING phase1 database

- **Back up the MySQL workhub DB first**. This ZIP contains no credential or reset script.
- Apply **only** `sql/v2_completion_migration.sql` once. It creates `ticket_audit_logs` and leaves existing data intact.
- **Do NOT run** `v2_fresh_schema.sql` on your populated DB; it is for new databases only.
- `application.properties` uses `${DB_PASSWORD}`, consistent with your existing user environment variable.
- Stop the old Spring Boot instance. Start the updated project from its root using `./mvnw.cmd spring-boot:run` on Windows PowerShell.
- Run tests with `./mvnw.cmd test`. Unit/integration results are to be reported based on the actual output, not this document.

## Curl example (Basic Auth, GET doesn't require CSRF)

```powershell
curl.exe -i -u 'Tom:123456' http://localhost:8080/tickets/16
```

For PUT/DELETE use the app's CSRF token and `If-Match` of the previously read Ticket version. Existing smoke tests that issue incomplete mutation requests may now expect `428` rather than the old outcome. Role checks still precede version checks, so non-Managers remain 403.

## Scope caveats / next verification

- Tests here prove mocked single-request behavior only. The provided `tests_concurrency.py` must be run against live MySQL/Redis (creates/soft-deletes its own test Ticket). A rollback test with isolated fixtures, cache race/load characterization, and full integration smoke run are still required.
- A bounded thread-pool notification facility is included (see `V2_ASYNC_NOTIFICATIONS.md`). The separate AI Agent project is not included here.
- Review that the frontend's `If-Match` header is included in all Ticket mutation paths before migrating users.


## Async notifications addition

See `V2_ASYNC_NOTIFICATIONS.md` for the latest notification worker, database migration and testing instructions. The original scope caveat about missing async notifications in an earlier draft is superseded by that document; AI Assistant remains separate and undelivered here.
