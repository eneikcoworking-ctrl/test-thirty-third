-- Create optimized indexes for retrieving the latest unread messages
CREATE INDEX idx_messages_unread_latest_desc ON messages (is_unread, created_at DESC, id DESC);
CREATE INDEX idx_messages_dialog_unread_latest_desc ON messages (dialog_id, is_unread, created_at DESC, id DESC);

-- Create an optimized database view returning active dialogs grouped/summarized by status
CREATE VIEW v_active_dialogs_by_status AS
SELECT
    d.status,
    COUNT(d.id) AS dialog_count,
    COALESCE(SUM(
        (SELECT COUNT(*) FROM messages m WHERE m.dialog_id = d.id AND m.is_unread = true)
    ), 0) AS total_unread_count
FROM dialogs d
WHERE d.is_active = true
GROUP BY d.status;
