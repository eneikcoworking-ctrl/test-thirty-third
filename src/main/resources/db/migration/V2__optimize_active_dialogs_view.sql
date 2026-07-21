-- Create composite index for optimal plan execution of the correlated subqueries with LIMIT 1
CREATE INDEX idx_messages_dialog_latest ON messages (dialog_id, created_at DESC, id DESC);

-- Drop the old view that used ROW_NUMBER() window function
DROP VIEW IF EXISTS v_active_dialogs;

-- Recreate the view using correlated subqueries with LIMIT 1 for optimal performance
CREATE VIEW v_active_dialogs AS
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
    (
        SELECT m.text
        FROM messages m
        WHERE m.dialog_id = d.id
        ORDER BY m.created_at DESC, m.id DESC
        LIMIT 1
    ) AS latest_message_text,
    (
        SELECT m.created_at
        FROM messages m
        WHERE m.dialog_id = d.id
        ORDER BY m.created_at DESC, m.id DESC
        LIMIT 1
    ) AS latest_message_time
FROM dialogs d
WHERE d.is_active = true;
