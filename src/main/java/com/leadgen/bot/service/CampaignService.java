package com.leadgen.bot.service;

import com.leadgen.bot.model.Campaign;
import com.leadgen.bot.model.Contact;
import com.leadgen.bot.repository.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;

    public CampaignService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Transactional
    public Campaign createCampaign(String name, String spintaxTemplate, List<String> contactInputs) {
        Campaign campaign = new Campaign(name, spintaxTemplate);
        if (contactInputs != null) {
            for (String input : contactInputs) {
                String trimmed = input.trim();
                if (trimmed.isEmpty()) continue;

                Contact contact = new Contact();

                // 1. Normalize and validate phone number (handling spaces, hyphens, dots, parens)
                String normalizedPhone = trimmed.replaceAll("[\\s\\-\\(\\)\\.]", "");
                if (normalizedPhone.matches("^\\+?[0-9]{10,15}$")) {
                    contact.setPhoneNumber(normalizedPhone);
                }
                // 2. Validate Telegram username format (starts with optional @, 5-32 characters, alphanumeric and underscore)
                else if (trimmed.matches("^@?[a-zA-Z0-9_]{5,32}$")) {
                    // Auto-prepend '@' to normalize unmatched usernames
                    String username = trimmed.startsWith("@") ? trimmed : "@" + trimmed;
                    contact.setUsername(username);
                }
                // 3. Otherwise, throw a clear validation exception for bad records
                else {
                    throw new IllegalArgumentException("Invalid contact format. Must be a valid phone number or username: " + trimmed);
                }

                contact.setStatus("PENDING");
                campaign.addContact(contact);
            }
        }
        return campaignRepository.save(campaign);
    }

    public Campaign getCampaign(Long id) {
        return campaignRepository.findById(id).orElse(null);
    }
}
