# WorkHub — Ticket Management System

WorkHub is a full-stack ticket management application built with **Java, Spring Boot, MyBatis, MySQL, and Redis**.

The project provides a structured workflow for creating, assigning, tracking, and resolving tickets. It focuses on practical backend engineering, including authentication, database transactions, dynamic queries, caching, input validation, and automated testing.

## Tech Stack

| Category | Technologies |
|---|---|
| Backend | Java 25, Spring Boot |
| Persistence | MyBatis, MySQL |
| Cache | Redis, Spring Data Redis |
| Security | Spring Security, Session Authentication, BCrypt, CSRF Protection |
| Testing | JUnit 5, Mockito, Spring Boot Test |
| Frontend | HTML, CSS, JavaScript |
| Build | Maven Wrapper |

## Key Features

### 1. Ticket Management

- Create, retrieve, update, and soft-delete tickets.
- Manage ticket status, priority, and assignee.
- Support ticket comments and status change history.
- Provide dynamic filtering by status and priority.
- Support pagination and controlled sorting of ticket lists.

### 2. Ticket Workflow

Tickets follow a predefined status workflow:

`OPEN → IN_PROGRESS → RESOLVED → CLOSED`

The backend validates status transitions and rejects invalid operations. The workflow also supports reopening a resolved ticket for further processing.

Ticket status changes are recorded in the database for tracking and auditing.

### 3. Authentication and Security

- Session-based authentication using Spring Security.
- Password hashing and verification using BCrypt.
- Password modification with current-password verification.
- CSRF protection for state-changing HTTP requests.
- Server-side identity validation to prevent clients from impersonating other users when creating tickets and comments.
- Authorization checks for account-related operations.

### 4. Redis Caching

WorkHub implements a Cache-Aside strategy for ticket detail queries.

- Ticket details are cached in Redis with a randomized 50-70-second TTL (TTL jitter); missing tickets have a 15-second negative-cache TTL.
- Cache hits avoid an additional ticket-detail query inside TicketService, but GET /tickets/{id} still queries MySQL through TicketAccessService.visibleTicket() for row-level authorization.
- Cache misses trigger a MySQL query and cache population.
- Cache invalidation is performed when relevant ticket data changes.
- Transactional status updates schedule cache invalidation after successful database commits.
- Redis connection failures are handled gracefully, allowing ticket queries to fall back to MySQL.

MySQL remains the source of truth.

### 5. Database and Transaction Management

- MyBatis is used for SQL mapping and database operations.
- Dynamic SQL supports optional ticket filtering conditions.
- Spring's `@Transactional` provides transaction management for related database operations.
- Ticket status updates and status history insertion are performed within the same transaction.
- Database operations are rolled back when an unhandled runtime exception causes transaction failure.

### 6. Validation and Error Handling

- Bean Validation checks required fields and field length constraints.
- Centralized exception handling provides consistent HTTP error responses.
- Invalid status transitions, missing resources, and malformed inputs are handled explicitly.

## Architecture

WorkHub follows a layered backend architecture.

**Controller → Service → Mapper → Database**

- **Controller Layer:** Handles HTTP requests, request parameters, authentication context, and API responses.
- **Service Layer:** Implements business rules, transaction boundaries, and caching logic.
- **Mapper Layer:** Uses MyBatis to perform database operations.
- **Database Layer:** Stores users, tickets, comments, and ticket status history.

Redis is integrated into the Service Layer to accelerate ticket detail retrieval.

The frontend communicates with the backend through HTTP APIs.

## API Examples

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/tickets` | Retrieve tickets with optional filters |
| `GET` | `/tickets/{id}` | Retrieve ticket details |
| `POST` | `/tickets` | Create a ticket |
| `PUT` | `/tickets/{id}/status` | Update ticket status |
| `GET` | `/csrf` | Retrieve a CSRF token |

Additional endpoints support ticket modifications, comments, user account operations, and authentication.

## Getting Started

### Requirements

- JDK 25
- MySQL
- Redis (for caching)
- Maven Wrapper (included in the project)

### Database Configuration

Configure the database connection in:

`src/main/resources/application.properties`

The database password is provided through the `DB_PASSWORD` environment variable rather than being hardcoded in the configuration.

A SQL dump is available at:

`sql/dump_workhub.sql`

### Run the Application

On Windows PowerShell, set the database password for the current terminal session:

`$env:DB_PASSWORD = "your_database_password"`

Run the Spring Boot application:

`.\mvnw.cmd spring-boot:run`

Start Redis separately to enable caching. Ticket detail queries are designed to continue using MySQL when Redis is unavailable.

## Testing

WorkHub has been tested using **JUnit 5, Mockito, Spring Boot integration tests, and manual API verification**.

### Automated Tests

**Validation Tests**

Verify required fields, field length restrictions, and valid ticket data.

**Service Unit Tests**

Use Mockito to isolate business logic and verify:

- Valid and invalid ticket status transitions.
- Behavior when a ticket does not exist.
- Database update and ticket history insertion calls.
- Relevant cache invalidation interactions.

**Transaction Integration Test**

Uses a real MySQL database to verify that an intentional runtime exception causes a ticket status update to be rolled back.

Run the automated test suite:

`.\mvnw.cmd test`

The integration test requires a running MySQL instance with suitable test data.

### Manual Verification

**Security**

- Session login and logout.
- CSRF-protected state-changing requests.
- Rejection of requests with missing or invalid CSRF tokens.
- Authentication and account authorization checks.

**Redis**

- Cache hit and cache miss behavior.
- Cache invalidation following ticket changes.
- Continued ticket retrieval when Redis is unavailable.

## Known Limitations

The current version focuses on demonstrating core backend functionality. Several areas remain available for future improvement.

1. **Transaction Test Coverage:** The rollback integration test verifies ticket status restoration. It does not yet independently assert rollback of ticket history records or the absence of `afterCommit()` callbacks after rollback.

2. **Cache Consistency:** Cache invalidation after transaction commit reduces inconsistency risks, but does not guarantee strong consistency during concurrent updates or Redis failures. Ticket detail cache entries use a randomized 50-70-second TTL, with 15-second negative caching for missing tickets.

3. **Test Isolation:** The current transaction integration test depends on an existing ticket with ID `14`. Future improvements should create and clean up dedicated test data automatically.

## Future Improvements

- Improve integration test isolation and transaction coverage.
- Add more automated tests for authentication and authorization.
- Strengthen Redis cache consistency and failure recovery.
- Introduce automated API testing and continuous integration.

## Project Purpose

WorkHub was developed as a practical Java backend project to apply concepts beyond basic CRUD.

Its main learning objectives include:

- Designing maintainable layered backend services.
- Implementing secure authentication and authorization.
- Managing database transactions and data consistency.
- Integrating Redis caching with failure handling.
- Testing business logic and database behavior.

The project emphasizes understanding backend architecture and engineering trade-offs rather than simply implementing API endpoints.