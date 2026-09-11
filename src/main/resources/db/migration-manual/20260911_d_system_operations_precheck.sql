-- Manual pre-check only. Review results before applying 20260911_d_system_operations.sql.
-- Do not delete or merge rows automatically when duplicates are returned.
SELECT app_user_id, notification_type, reference_type, reference_id, COUNT(*) AS duplicate_count
FROM notification
GROUP BY app_user_id, notification_type, reference_type, reference_id
HAVING COUNT(*) > 1;

-- Confirm the notification unique constraint does not already exist.
SELECT constraint_name
FROM information_schema.table_constraints
WHERE table_schema = DATABASE()
  AND table_name = 'notification'
  AND constraint_type = 'UNIQUE'
  AND constraint_name = 'uk_notification_event';
