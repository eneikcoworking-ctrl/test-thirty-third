CREATE TABLE telegram_accounts (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(50),
    username VARCHAR(100),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dialogs (
    id BIGSERIAL PRIMARY KEY,
    telegram_account_id BIGINT NOT NULL,
    peer_id VARCHAR(100) NOT NULL,
    peer_username VARCHAR(100),
    peer_phone_number VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dialog_telegram_account FOREIGN KEY (telegram_account_id) REFERENCES telegram_accounts(id) ON DELETE CASCADE
);

CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    dialog_id BIGINT NOT NULL,
    sender_id VARCHAR(100) NOT NULL,
    is_from_me BOOLEAN NOT NULL DEFAULT FALSE,
    is_unread BOOLEAN NOT NULL DEFAULT TRUE,
    text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_dialog FOREIGN KEY (dialog_id) REFERENCES dialogs(id) ON DELETE CASCADE
);

-- Optimized indexes for fast lookup and unread counts
CREATE INDEX idx_dialogs_active_status ON dialogs (is_active, status);
CREATE INDEX idx_messages_unread_lookup ON messages (is_unread, created_at DESC);
CREATE INDEX idx_messages_dialog_unread ON messages (dialog_id, is_unread);
CREATE INDEX idx_messages_dialog_latest ON messages (dialog_id, created_at DESC, id DESC);

-- Optimized database view using correlated subqueries for optimal query execution plan with indexes
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
    (
        SELECT COUNT(*)
        FROM messages m
        WHERE m.dialog_id = d.id AND m.is_unread = true
    ) AS unread_count,
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
