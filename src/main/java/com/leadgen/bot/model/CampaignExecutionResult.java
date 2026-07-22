package com.leadgen.bot.model;

import java.util.ArrayList;
import java.util.List;

public class CampaignExecutionResult {
    private Long campaignId;
    private String status;
    private int messagesSent;
    private int rotationsCount;
    private List<String> logs = new ArrayList<>();

    public CampaignExecutionResult() {}

    public CampaignExecutionResult(Long campaignId, String status, int messagesSent, int rotationsCount, List<String> logs) {
        this.campaignId = campaignId;
        this.status = status;
        this.messagesSent = messagesSent;
        this.rotationsCount = rotationsCount;
        this.logs = logs;
    }

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
