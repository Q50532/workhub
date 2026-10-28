> NOTE: This was the **phase1** README. For the new completion increment and the database migration, read `V2_COMPLETION_README.md` first. The password variable used by this project is now `DB_PASSWORD`.

# WorkHub V2 — First engineering increment (review build)

> **NOT a verified release**. The downloaded source was inspected and modified, but this execution environment has Java 21, no Maven installation or local MySQL/Redis. Compile and integration testing with the project's Java 25 / Spring Boot 4.1.1 setup are still required. Never claim untested functions are production-ready or list them on a résumé as validated.

## Current changes (source implementation; integration NOT confirmed)

- `users.role`: `USER` / `MANAGER`, `CustomUserDetailsService` authorities and `/api/me` role.
- Ticket listing query filtered by `created_by OR assigned_to` for non-managers, with matching paginated COUNT; MANAGER may see all tickets.
- Ticket read, comments and logs checked for access; creator or manager may edit/delete, only assignee or manager may change status, manager-only assignment and priority updates. Existing creator-only comment Resolution behavior retained.
- Ticket status, description, priority, assignee, and soft-delete updates use `tickets.version` conditional updates. Failed conditional updates produce HTTP 409; status logs remain transaction-bound, cache invalidation scheduled post-commit.
- Basic negative-cache TTL (15 seconds) and jittered positive cache TTL (50–70 seconds) for Ticket detail. This **does not** fully solve hotspot cache breakdown or Cache-Aside race windows.
- Frontend shows manager assignment/priority controls selectively. Frontend visibility is not a security barrier; server performs checks.
- Added pure/mock unit tests for authorization and conflict paths, **not yet executed with Maven**.

## Database migration — EXISTING database

1. **Back up** your `workhub` MySQL database first.
2. Run `sql/v2_migration.sql` **once** on the existing DB; do NOT run `v2_fresh_schema.sql` on a populated DB.
3. An existing user remains `USER` by default. To promote an intended manager, explicitly run e.g.

```sql
UPDATE users SET role = 'MANAGER' WHERE username = 'tom';
```

4. Migration is not automatically executed. The code needs the new `role` / `version` columns; without migration it will not work correctly.
5. Assigning a manager role does **not** change an existing browser session's stored Spring Security authorities; log out and log back in after the DB change. Authorization checks themselves consult the database role.

## Password config

The original ZIP contained a local DB password in `application.properties`. This distribution replaces it with `${DB_PASSWORD}`. In Windows PowerShell, set your actual local DB password before starting Spring Boot:

```powershell
$env:DB_PASSWORD = Read-Host "WorkHub MySQL password"
.\mvnw.cmd spring-boot:run
```

Keep real credentials out of git and future ZIP attachments. Review your original ZIP's `.git` history before sharing it elsewhere; replacing the properties file in this distribution does not erase older copies.

## Recommended verification on your Windows machine

```powershell
# From the WorkHub root, on Java 25
$env:DB_PASSWORD = Read-Host "WorkHub MySQL password"
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Important: The previous `TicketTransactionTest` is tied to a specific local Ticket ID 14 and requires a seeded MySQL database. Do not interpret failures caused by missing seed data as proof the concurrency logic is wrong; replace with isolated test fixtures later.

### Manual regression tests required

- As a `USER`, `/tickets` includes only self-created or assigned tickets; try opening a different ticket by ID and expect 403.
- As a `USER`, assignment and priority mutations return 403. As `MANAGER`, they work.
- A creator can mark Resolution; non-creator who can view the ticket cannot.
- Two updates based on the same `version` cause one update to fail with 409 and do not create an incorrect status history row.
- After a committed mutation, re-read the ticket to confirm Redis does not indefinitely return the old value.

## What remains before the frozen V2 P0 scope can be called complete

- Complete assignment/priority Audit Log with actor attribution, stronger workflow rules, comprehensive automated integration and concurrency tests, and verified transaction rollback.
- Cache breakdown protection and a reproducible load/performance report.
- Agent resolution/priority suggestions as explicit tested output formats, approved-resolution review workflow, and automated knowledge synchronization.
- End-to-end Agent Tool Calling with a live Gemini API and running WorkHub.

See the separate `ai-assistant/README_V2.md` for the preliminary local Agent.
