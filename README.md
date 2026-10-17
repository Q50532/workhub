## Testing

The WorkHub backend has been verified using JUnit 5, Mockito, and manual API testing.

**Completed verification:**

- **Validation Tests:** Verify required fields, field length constraints, and valid Ticket data.
- **Service Unit Tests:** Verify valid and invalid Ticket status transitions, missing Tickets, status update calls, TicketLog insertion calls, and Redis cache invalidation calls.
- **Transaction Integration Test:** Verify that a forced RuntimeException causes MySQL to roll back a Ticket status update.
- **Spring Security:** Verify Session authentication, CSRF-protected requests, and rejection of requests with missing or invalid CSRF Tokens.
- **Redis:** Verify cache hits, cache misses, and graceful degradation when Redis is unavailable.

Run automated tests using:

`.\mvnw.cmd test`

The transaction integration test requires a running MySQL database and currently depends on an existing test Ticket.

## Known Limitations

1. **Transaction rollback coverage:** The integration test verifies that a Ticket's status is restored after Rollback. It does not separately assert that TicketLog insertions are rolled back or that `afterCommit()` callbacks are skipped during Rollback.

2. **Cache consistency:** Redis cache invalidation is scheduled after successful transaction commits where configured. This improves consistency but does not guarantee strong consistency under all concurrent operations or Redis failures. Ticket cache entries have a 60-second TTL.

3. **Integration test data:** The current transaction integration test uses a fixed Ticket ID (`14`). It requires suitable existing test data and is not yet fully isolated from the development database.