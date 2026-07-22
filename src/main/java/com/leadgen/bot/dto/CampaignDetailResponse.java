package com.leadgen.bot.dto;

import java.time.LocalDateTime;

public class CampaignDetailResponse {
    private Long id;
    private String name;
    private String spintaxTemplate;
    private LocalDateTime createdAt;
    private CampaignStatsResponse stats;

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

    public CampaignStatsResponse getStats() {
        return stats;
    }

    public void setStats(CampaignStatsResponse stats) {
        this.stats = stats;
    }
}
