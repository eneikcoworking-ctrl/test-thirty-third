DROP VIEW IF EXISTS v_active_dialogs;
DROP INDEX IF EXISTS idx_messages_dialog_unread;
DROP INDEX IF EXISTS idx_messages_unread_lookup;
DROP INDEX IF EXISTS idx_dialogs_active_status;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS dialogs;
DROP TABLE IF EXISTS telegram_accounts;
