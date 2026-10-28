-- Additional migration for an EXISTING V2 phase1 database. Run ONCE after backup.
-- This does NOT modify existing users, Tickets, or passwords.
USE workhub;
CREATE TABLE IF NOT EXISTS ticket_audit_logs (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ticket_id INT NOT NULL,
    actor_id INT NOT NULL,
    action VARCHAR(30) NOT NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_audit_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id),
    CONSTRAINT fk_ticket_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id),
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
