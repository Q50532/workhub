-- Run in MySQL Workbench; collect actual rows/cost before deciding new indexes.
USE workhub;
EXPLAIN ANALYZE SELECT * FROM tickets WHERE deleted = FALSE
  AND status = 'OPEN' ORDER BY id ASC LIMIT 10;
EXPLAIN ANALYZE SELECT * FROM tickets WHERE deleted = FALSE
  AND (created_by = 2 OR assigned_to = 2)
  ORDER BY id ASC LIMIT 10;
EXPLAIN ANALYZE SELECT COUNT(*) FROM tickets WHERE deleted = FALSE
  AND status = 'OPEN';
EXPLAIN ANALYZE SELECT s.ticket_id FROM ticket_sla s
  JOIN tickets t ON t.id = s.ticket_id
  WHERE s.escalated_at IS NULL AND s.due_at <= NOW()
    AND t.deleted = FALSE AND t.status IN ('OPEN','IN_PROGRESS')
  ORDER BY s.due_at LIMIT 100;
-- Do NOT claim improvements without recording BEFORE/AFTER execution plans.
