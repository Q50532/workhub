# WorkHub V2 — bounded asynchronous notifications (source addition)

## Scope and behavior

Ticket status transitions and changes of assignee now create **durable notification work records** in `ticket_notifications`. These inserts occur **in the same MySQL transaction** as the Ticket mutation, status log and audit log. A rolled-back Ticket change cannot leave an orphan notification.

`NotificationService` schedules each committed record on a bounded `ThreadPoolTaskExecutor` (core 2 / max 4 / waiting queue 100). The worker atomically claims the row, renders a readable message, and marks it READY for the recipient's inbox. If the executor is saturated or the app restarts, the scheduled 10-second recovery job scans PENDING/stale jobs and retries. A failed row is retried up to 10 claims; unresolved jobs need manual review. Concurrent workers cannot claim the same non-stale job.

Recipient selection:
- Status change: notify the Ticket creator and assignee, excluding the actor and removing duplicates.
- Assignment change: notify the newly assigned user (except self-assignment).
- The inbox endpoint `GET /api/notifications?limit=20` derives recipient from the authenticated user, not query parameters. The frontend page is `/notifications.html`.
- There is **no email/SMS integration**. This is an asynchronous in-app notification pipeline, not a claim of external message delivery or distributed message queue guarantees.

## Upgrade an existing Phase 1 workhub database

1. Back up `workhub` database and stop the old server.
2. If the completion migration has NOT been run before: apply `sql/v2_completion_migration.sql` (contains both audit table and notification table creation). SQL uses `IF NOT EXISTS` for these tables. Do not rerun `v2_migration.sql`.
3. If the completion migration HAS been run before: apply `sql/v2_async_notifications_only.sql` instead. It creates the notification table without changing other data.
4. Start from project root: `./mvnw.cmd spring-boot:run` (PowerShell). The DB password uses `DB_PASSWORD` as before.
5. Run `./mvnw.cmd test`. Existing tests plus `NotificationOutboxTest` and `NotificationWorkerTest` run without connecting to real MySQL.
6. In browser sign in, change a Ticket status or assign a Ticket to another user (the action must be authorized). The recipient logs in and opens `/notifications.html`. Ready notification may appear after a short delay. The sender doesn't receive their own notification.

Do not run `sql/v2_fresh_schema.sql` on an existing populated database.

## What to test in Windows environment

- Successful status change and assignee change; recipient sees a notification.
- Rollback/conflict 409: no new notification row after failed update.
- USER cannot request notifications for someone else; the GET endpoint has no recipient id parameter.
- Disable the async executor or restart the app after a mutation: pending rows should be recovered.
- Observe logs for unexpected retries or FAILED rows (`SELECT id,status,attempts FROM ticket_notifications WHERE status <> 'READY';`).

## Limitations

- This is source code delivered for integration, **not a claim that live Windows/Java 25 + MySQL + Redis tests were executed here**.
- The outbox is persisted in MySQL but there is no external broker, distributed queue or cross-service exactly-once delivery.
- Worker claims are protected by MySQL, and 2-minute recovery lease assumes message formatting takes much less than two minutes. Do not use this unchanged for a long-running external message provider.
- On persistent backend errors, rows can enter FAILED and require manual intervention; add metrics / operator retry endpoints before production deployment.
- Source archive does not include the separate AI Assistant project.
