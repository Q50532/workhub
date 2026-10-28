-- WorkHub Plus additions for an EXISTING async V2 database. Run ONCE after backup.
-- No existing user passwords or Tickets are modified. Requires ticket_notifications table from V2 async migration.
USE workhub;

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
