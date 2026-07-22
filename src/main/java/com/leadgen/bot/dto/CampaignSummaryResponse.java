package com.leadgen.bot.dto;

import java.time.LocalDateTime;

public class CampaignSummaryResponse {
    private Long id;
    private String name;
    private String spintaxTemplate;
    private LocalDateTime createdAt;
    private int totalContactsCount;
    private int pendingContactsCount;
    private int dispatchedMessagesCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpintaxTemplate() {
        return spintaxTemplate;
    }

    public void setSpintaxTemplate(String spintaxTemplate) {
        this.spintaxTemplate = spintaxTemplate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getTotalContactsCount() {
        return totalContactsCount;
    }

    public void setTotalContactsCount(int totalContactsCount) {
        this.totalContactsCount = totalContactsCount;
    }

    public int getPendingContactsCount() {
        return pendingContactsCount;
    }

    public void setPendingContactsCount(int pendingContactsCount) {
        this.pendingContactsCount = pendingContactsCount;
    }

    public int getDispatchedMessagesCount() {
        return dispatchedMessagesCount;
    }

    public void setDispatchedMessagesCount(int dispatchedMessagesCount) {
        this.dispatchedMessagesCount = dispatchedMessagesCount;
    }
}
