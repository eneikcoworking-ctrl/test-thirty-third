package com.leadgen.bot;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "v_active_dialogs")
public class ActiveDialogSummary {

    @Id
    @Column(name = "dialog_id")
    private Long dialogId;

    @Column(name = "telegram_account_id", nullable = false)
    private Long telegramAccountId;

    @Column(name = "peer_id", nullable = false, length = 100)
    private String peerId;

    @Column(name = "peer_username", length = 100)
    private String peerUsername;

    @Column(name = "peer_phone_number", length = 50)
    private String peerPhoneNumber;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "unread_count")
    private Long unreadCount;

    @Column(name = "latest_message_text")
    private String latestMessageText;

    @Column(name = "latest_message_time")
    private LocalDateTime latestMessageTime;

    // Getters
    public Long getDialogId() {
        return dialogId;
    }

    public Long getTelegramAccountId() {
        return telegramAccountId;
    }

    public String getPeerId() {
        return peerId;
    }

    public String getPeerUsername() {
        return peerUsername;
    }

    public String getPeerPhoneNumber() {
        return peerPhoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getUnreadCount() {
        return unreadCount;
    }

    public String getLatestMessageText() {
        return latestMessageText;
    }

    public LocalDateTime getLatestMessageTime() {
        return latestMessageTime;
    }
}
