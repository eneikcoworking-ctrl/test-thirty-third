package com.leadgen.bot.service;

import com.leadgen.bot.TelegramAccount;
import com.leadgen.bot.TelegramAccountRepository;
import com.leadgen.bot.exception.DailyLimitExceededException;
import com.leadgen.bot.exception.FloodLimitException;
import com.leadgen.bot.model.Campaign;
import com.leadgen.bot.model.CampaignExecutionResult;
import com.leadgen.bot.model.Contact;
import com.leadgen.bot.repository.CampaignRepository;
import com.leadgen.bot.repository.ContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CampaignOrchestrationService {

    private final CampaignRepository campaignRepository;
    private final ContactRepository contactRepository;
    private final TelegramAccountRepository telegramAccountRepository;
    private final TelegramService telegramService;
    private final MessageLogService messageLogService;

    public CampaignOrchestrationService(
            CampaignRepository campaignRepository,
            ContactRepository contactRepository,
            TelegramAccountRepository telegramAccountRepository,
            TelegramService telegramService,
            MessageLogService messageLogService) {
        this.campaignRepository = campaignRepository;
        this.contactRepository = contactRepository;
        this.telegramAccountRepository = telegramAccountRepository;
        this.telegramService = telegramService;
        this.messageLogService = messageLogService;
    }

    @Transactional
    public CampaignExecutionResult executeCampaign(Long campaignId, List<Long> accountIds) {
        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) {
            List<String> logs = new ArrayList<>();
            logs.add("Campaign not found with ID: " + campaignId);
            return new CampaignExecutionResult(campaignId, "NOT_FOUND", 0, 0, logs);
        }

        // Fetch selected accounts and keep only active ones
        List<TelegramAccount> allAccounts = telegramAccountRepository.findAllById(accountIds);
        List<TelegramAccount> activeAccounts = allAccounts.stream()
                .filter(acc -> "ACTIVE".equalsIgnoreCase(acc.getStatus()))
                .collect(Collectors.toList());

        List<String> logs = new ArrayList<>();
        int messagesSent = 0;
        int rotationsCount = 0;
        String executionStatus = "COMPLETED";

        if (activeAccounts.isEmpty()) {
            logs.add("No active Telegram accounts available for execution.");
            return new CampaignExecutionResult(campaignId, "SUSPENDED", 0, 0, logs);
        }

        // Filter contacts to process only PENDING ones
        List<Contact> pendingContacts = campaign.getContacts().stream()
                .filter(contact -> "PENDING".equalsIgnoreCase(contact.getStatus()))
                .collect(Collectors.toList());

        if (pendingContacts.isEmpty()) {
            logs.add("No pending contacts in this campaign to process.");
            return new CampaignExecutionResult(campaignId, "COMPLETED", 0, 0, logs);
        }

        int currentAccountIdx = 0;

        for (Contact contact : pendingContacts) {
            boolean messageSent = false;

            while (!messageSent && !activeAccounts.isEmpty()) {
                // Ensure index is within bounds
                if (currentAccountIdx >= activeAccounts.size()) {
                    currentAccountIdx = 0;
                }

                TelegramAccount account = activeAccounts.get(currentAccountIdx);
                String messageText = SpintaxParser.parse(campaign.getSpintaxTemplate());

                try {
                    String identifier = contact.getUsername() != null ? contact.getUsername() : contact.getPhoneNumber();
                    telegramService.sendMessage(account, identifier, messageText);

                    // Message sent successfully!
                    messageLogService.logMessage(campaign, contact, account.getUsername() != null ? account.getUsername() : account.getPhoneNumber(), "DELIVERED");
                    contact.setStatus("COMPLETED");
                    contactRepository.save(contact);
                    messageSent = true;
                    messagesSent++;

                    logs.add("Sent message to " + identifier + " using account " + account.getUsername());
                } catch (FloodLimitException e) {
                    logs.add("Account " + account.getUsername() + " hit flood limit. Status updated to FLOOD_LIMIT. Rotating.");
                    rotationsCount++;

                    // Mark account status as FLOOD_LIMIT
                    account.setStatus("FLOOD_LIMIT");
                    telegramAccountRepository.save(account);

                    // Remove from active pool for this run
                    activeAccounts.remove(currentAccountIdx);
                    // No index increment needed as the list shrank
                } catch (DailyLimitExceededException e) {
                    logs.add("Account " + account.getUsername() + " hit daily limit. Status updated to LIMIT_REACHED. Rotating.");
                    rotationsCount++;

                    // Mark account status as LIMIT_REACHED
                    account.setStatus("LIMIT_REACHED");
                    telegramAccountRepository.save(account);

                    // Remove from active pool for this run
                    activeAccounts.remove(currentAccountIdx);
                    // No index increment needed as the list shrank
                }
            }

            if (!messageSent) {
                logs.add("Campaign execution suspended: no active Telegram accounts remaining in the rotation pool.");
                executionStatus = "SUSPENDED";
                break;
            }
        }

        return new CampaignExecutionResult(campaignId, executionStatus, messagesSent, rotationsCount, logs);
    }
}
