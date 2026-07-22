package com.leadgen.bot.dto;

public class CampaignUpdateRequest {
    private String name;
    private String spintaxTemplate;

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
}
