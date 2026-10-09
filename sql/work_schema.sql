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