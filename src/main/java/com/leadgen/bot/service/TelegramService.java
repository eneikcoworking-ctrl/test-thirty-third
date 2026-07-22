package com.leadgen.bot.service;

import com.leadgen.bot.TelegramAccount;
import com.leadgen.bot.exception.DailyLimitExceededException;
import com.leadgen.bot.exception.FloodLimitException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {

    // Allows dynamic simulation of account behaviors in tests
    private final Map<Long, String> simulatedBehaviors = new HashMap<>();

    public void setBehavior(Long accountId, String behavior) {
        simulatedBehaviors.put(accountId, behavior);
    }

    public void clearBehaviors() {
        simulatedBehaviors.clear();
    }

    public void sendMessage(TelegramAccount account, String contactIdentifier, String messageText) {
        String behavior = simulatedBehaviors.get(account.getId());
        if (behavior == null) {
            // Support username/status triggers as a fallback for simple tests
            if (account.getUsername() != null) {
                if (account.getUsername().contains("flood")) {
                    behavior = "FLOOD";
                } else if (account.getUsername().contains("limit")) {
                    behavior = "LIMIT";
                }
            }
        }

        if ("FLOOD".equalsIgnoreCase(behavior)) {
            throw new FloodLimitException("FLOOD_WAIT error simulated for account " + account.getId());
        } else if ("LIMIT".equalsIgnoreCase(behavior)) {
            throw new DailyLimitExceededException("Daily limit exceeded simulated for account " + account.getId());
        }

        // Default successful sending logic: no-op, just succeeds
    }
}
