-- Recreate the original view that used ROW_NUMBER() window function
DROP VIEW IF EXISTS v_active_dialogs;

CREATE VIEW v_active_dialogs AS
WITH latest_msgs AS (
    SELECT dialog_id, text, created_at,
           ROW_NUMBER() OVER (PARTITION BY dialog_id ORDER BY created_at DESC, id DESC) as rn
    FROM messages
)
SELECT
    d.id AS dialog_id,
    d.telegram_account_id,
    d.peer_id,
    d.peer_username,
    d.peer_phone_number,
    d.status,
    d.is_active,
    d.created_at,
    d.updated_at,
    COALESCE((
        SELECT COUNT(*)
        FROM messages m
        WHERE m.dialog_id = d.id AND m.is_unread = true
    ), 0) AS unread_count,
    lm.text AS latest_message_text,
    lm.created_at AS latest_message_time
FROM dialogs d
LEFT JOIN latest_msgs lm ON d.id = lm.dialog_id AND lm.rn = 1
WHERE d.is_active = true;

-- Drop composite index
DROP INDEX IF EXISTS idx_messages_dialog_latest;
