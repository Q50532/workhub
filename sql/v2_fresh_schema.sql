-- Clean WorkHub V2 schema (for a NEW / EMPTY database only).
 CREATE DATABASE IF NOT EXISTS workhub;
 USE workhub;
 CREATE TABLE users (
   id INT AUTO_INCREMENT PRIMARY KEY,
   username VARCHAR(50) NOT NULL UNIQUE,
   email VARCHAR(100) NOT NULL,
   password VARCHAR(255) NOT NULL,
   role VARCHAR(20) NOT NULL DEFAULT 'USER'
 );
 CREATE TABLE tickets (
   id INT AUTO_INCREMENT PRIMARY KEY,
   title VARCHAR(150) NOT NULL,
   description TEXT NOT NULL,
   category VARCHAR(50) NOT NULL,
   created_by INT NOT NULL,
   assigned_to INT NULL,
   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
   status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
   resolution TEXT,
   priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
   deleted BOOLEAN NOT NULL DEFAULT FALSE,
   version INT NOT NULL DEFAULT 0,
   FOREIGN KEY (created_by) REFERENCES users(id),
   FOREIGN KEY (assigned_to) REFERENCES users(id)
 );
 CREATE TABLE comments (
   id INT AUTO_INCREMENT PRIMARY KEY,
   ticket_id INT NOT NULL,
   author_id INT NOT NULL,
   content TEXT NOT NULL,
   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
   is_resolution TINYINT(1) NOT NULL DEFAULT 0,
   FOREIGN KEY (ticket_id) REFERENCES tickets(id),
   FOREIGN KEY (author_id) REFERENCES users(id)
 );
 CREATE TABLE ticket_logs (
   id INT AUTO_INCREMENT PRIMARY KEY,
   ticket_id INT NOT NULL,
   old_status VARCHAR(30) NOT NULL,
   new_status VARCHAR(30) NOT NULL,
   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
   FOREIGN KEY (ticket_id) REFERENCES tickets(id)
 );
 CREATE INDEX idx_tickets_status_deleted ON tickets(status,deleted);
 CREATE INDEX idx_tickets_creator_deleted ON tickets(created_by, deleted);
 CREATE INDEX idx_tickets_assignee_deleted ON tickets(assigned_to, deleted);

CREATE TABLE IF NOT EXISTS ticket_audit_logs (
   id INT AUTO_INCREMENT PRIMARY KEY,
   ticket_id INT NOT NULL,
   actor_id INT NOT NULL,
   action VARCHAR(30) NOT NULL,
   old_value TEXT NULL,
   new_value TEXT NULL,
   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
   FOREIGN KEY (ticket_id) REFERENCES tickets(id),
   FOREIGN KEY (actor_id) REFERENCES users(id),
   INDEX idx_ticket_audit_ticket_time (ticket_id, created_at)
);


-- Durable queue for asynchronous Ticket notifications.
-- A row is inserted in the same transaction as a Ticket status/assignee update.
-- The bounded worker marks the recipient's inbox item READY after commit.
CREATE TABLE IF NOT EXISTS ticket_notifications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ticket_id INT NOT NULL,
    recipient_id INT NOT NULL,
    actor_id INT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    old_value VARCHAR(100) NULL,
    new_value VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message VARCHAR(500) NULL,
    attempts INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_started_at DATETIME NULL,
    processed_at DATETIME NULL,
    CONSTRAINT fk_notification_ticket FOREIGN KEY(ticket_id) REFERENCES tickets(id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY(recipient_id) REFERENCES users(id),
    CONSTRAINT fk_notification_actor FOREIGN KEY(actor_id) REFERENCES users(id),
    INDEX idx_notification_recipient_ready (recipient_id, status, id),
    INDEX idx_notification_status (status, id)
);

-- WorkHub Plus additions for an EXISTING async V2 database. Run ONCE after backup.
-- No existing user passwords or Tickets are modified. Requires ticket_notifications table from V2 async migration.


CREATE TABLE IF NOT EXISTS ticket_create_requests (
  actor_id INT NOT NULL,
  request_key VARCHAR(128) NOT NULL,
  request_hash CHAR(64) NOT NULL,
  ticket_id INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (actor_id, request_key),
  CONSTRAINT fk_ticket_create_actor FOREIGN KEY (actor_id) REFERENCES users(id),
  CONSTRAINT fk_ticket_create_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id)
);

CREATE TABLE IF NOT EXISTS ticket_sla (
  ticket_id INT NOT NULL PRIMARY KEY,
  due_at DATETIME NOT NULL,
  escalated_at DATETIME NULL,
  CONSTRAINT fk_ticket_sla_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id),
  INDEX idx_ticket_sla_due (escalated_at, due_at)
);

-- Backfill existing Tickets using current priority. Existing Tickets start SLA at migration time.
INSERT INTO ticket_sla(ticket_id, due_at)
SELECT id, DATE_ADD(NOW(), INTERVAL CASE priority
    WHEN 'URGENT' THEN 60 WHEN 'HIGH' THEN 240 WHEN 'LOW' THEN 4320 ELSE 1440 END MINUTE)
FROM tickets
ON DUPLICATE KEY UPDATE ticket_id=ticket_id;
