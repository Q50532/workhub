-- WorkHub V2: add ownership fencing for Notification outbox workers.
-- EXISTING DATABASE ONLY. Back up the database first. Run ONCE.
-- Stop the WorkHub service while deploying migration and Java changes together.
USE workhub;

ALTER TABLE ticket_notifications
    ADD COLUMN claim_token CHAR(36) NULL;

-- Old PROCESSING jobs created before deployment have claim_token = NULL.
-- Let them be recovered only after the two-minute lease has expired.
-- No existing notification data is deleted or reset.

-- Verification (should return exactly one row):
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ticket_notifications'
  AND COLUMN_NAME = 'claim_token';
