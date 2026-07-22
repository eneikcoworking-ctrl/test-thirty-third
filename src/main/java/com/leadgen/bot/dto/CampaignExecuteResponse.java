package com.leadgen.bot.dto;

import java.util.List;

public class CampaignExecuteResponse {
    private Long campaignId;
    private String status;
    private int messagesSent;
    private int rotationsCount;
    private List<String> logs;

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getMessagesSent() {
        return messagesSent;
    }

    public void setMessagesSent(int messagesSent) {
        this.messagesSent = messagesSent;
    }

    public int getRotationsCount() {
        return rotationsCount;
    }

    public void setRotationsCount(int rotationsCount) {
        this.rotationsCount = rotationsCount;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }
}
