-- Drop the view
DROP VIEW IF EXISTS v_active_dialogs_by_status;

-- Drop the indexes
DROP INDEX IF EXISTS idx_messages_dialog_unread_latest_desc;
DROP INDEX IF EXISTS idx_messages_unread_latest_desc;
