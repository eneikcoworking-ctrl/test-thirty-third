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

-- Optimized indexes
CREATE INDEX idx_dialogs_active_status ON dialogs (is_active, status);
CREATE INDEX idx_messages_unread_lookup ON messages (is_unread, created_at DESC);
CREATE INDEX idx_messages_dialog_unread ON messages (dialog_id, is_unread, created_at DESC);

-- Optimized database view
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
