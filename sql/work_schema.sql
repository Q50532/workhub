-- WorkHub initial schema
-- Only use this file when creating a fresh database.
-- Do not run the whole file against an existing WorkHub database.

CREATE DATABASE IF NOT EXISTS workhub;
USE workhub;


-- =========================
-- users
-- =========================

CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL
);


-- =========================
-- tickets
-- =========================

CREATE TABLE tickets (
    id INT PRIMARY KEY AUTO_INCREMENT,

    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,

    created_by INT NOT NULL,
    assigned_to INT,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    resolution TEXT,

    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',

    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_ticket_creator
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_ticket_assignee
        FOREIGN KEY (assigned_to)
        REFERENCES users(id)
);


-- =========================
-- comments
-- =========================

CREATE TABLE comments (
    id INT PRIMARY KEY AUTO_INCREMENT,

    ticket_id INT NOT NULL,
    author_id INT NOT NULL,

    content TEXT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_comment_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES tickets(id),

    CONSTRAINT fk_comment_author
        FOREIGN KEY (author_id)
        REFERENCES users(id)
);


-- =========================
-- ticket_logs
-- =========================

CREATE TABLE ticket_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,

    ticket_id INT NOT NULL,

    old_status VARCHAR(30) NOT NULL,
    new_status VARCHAR(30) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ticket_log_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES tickets(id)
);

CREATE INDEX idx_tickets_status_deleted
ON tickets(status, deleted);

ALTER TABLE users
ADD COLUMN password VARCHAR(255);
DESCRIBE users;

SHOW CREATE TABLE comments;

ALTER TABLE comments
ADD COLUMN is_resolution TINYINT(1) NOT NULL DEFAULT 0;

SELECT id, ticket_id, content, is_resolution
FROM comments
ORDER BY ticket_id, id;

Show Columns From users;

select * from users;

SELECT
    t.id AS ticket_id,
    t.created_by,
    c.id AS comment_id
FROM tickets t
JOIN comments c ON t.id = c.ticket_id
WHERE t.created_by = 2;

SELECT id, title, created_by
FROM tickets
WHERE created_by = 2;

UPDATE users
SET password = '$2y$10$d1hDfkXwFFyRiy83hqYZ0uBeEcZPHc0Gyfk9hn1beuI2eObfaY19i'
WHERE id = 2
  AND username = 'Alice'
  AND password IS NULL;