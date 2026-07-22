package com.leadgen.bot.dto;

public class CampaignStatsResponse {
    private int totalContacts;
    private int pendingContacts;
    private int invalidContacts;
    private int deliveredCount;
    private int failedCount;

    public int getTotalContacts() {
        return totalContacts;
    }

    public void setTotalContacts(int totalContacts) {
        this.totalContacts = totalContacts;
    }

    public int getPendingContacts() {
        return pendingContacts;
    }

    public void setPendingContacts(int pendingContacts) {
        this.pendingContacts = pendingContacts;
    }

    public int getInvalidContacts() {
        return invalidContacts;
    }

    public void setInvalidContacts(int invalidContacts) {
        this.invalidContacts = invalidContacts;
    }

    public int getDeliveredCount() {
        return deliveredCount;
    }

    public void setDeliveredCount(int deliveredCount) {
        this.deliveredCount = deliveredCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }
}
