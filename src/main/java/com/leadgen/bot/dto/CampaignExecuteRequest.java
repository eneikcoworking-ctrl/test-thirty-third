package com.leadgen.bot.dto;

import java.util.List;

public class CampaignExecuteRequest {
    private List<Long> accountIds;

    public List<Long> getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(List<Long> accountIds) {
        this.accountIds = accountIds;
    }
}
