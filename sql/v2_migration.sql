-- WorkHub V2 migration for existing MySQL database.
-- Back up your database before executing. Execute ONCE, then start the V2 application.
 USE workhub;
 ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
 ALTER TABLE tickets ADD COLUMN version INT NOT NULL DEFAULT 0;
 CREATE INDEX idx_tickets_creator_deleted ON tickets(created_by, deleted);
 CREATE INDEX idx_tickets_assignee_deleted ON tickets(assigned_to, deleted);
 -- Grant manager role deliberately; DO NOT automatically promote every user.
 -- UPDATE users SET role = 'MANAGER' WHERE username = 'tom';

USE workhub;

-- Verify migration
SHOW COLUMNS FROM users LIKE 'role';
SHOW COLUMNS FROM tickets LIKE 'version';

-- Set Tom as Manager
UPDATE users
SET role = 'MANAGER'
WHERE username = 'tom';

-- Verify users
SELECT id, username, role
FROM users;

USE workhub;

SELECT id, username, role
FROM users;