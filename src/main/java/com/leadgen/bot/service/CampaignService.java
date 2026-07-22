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
                if (trimmed.startsWith("@")) {
                    contact.setUsername(trimmed);
                } else if (trimmed.matches("^\\+?[0-9]{10,15}$")) {
                    contact.setPhoneNumber(trimmed);
                } else {
                    contact.setUsername(trimmed);
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

    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    @Transactional
    public Campaign updateCampaign(Long id, String name, String spintaxTemplate) {
        Campaign campaign = campaignRepository.findById(id).orElse(null);
        if (campaign != null) {
            campaign.setName(name);
            campaign.setSpintaxTemplate(spintaxTemplate);
            return campaignRepository.save(campaign);
        }
        return null;
    }

    @Transactional
    public void deleteCampaign(Long id) {
        campaignRepository.deleteById(id);
    }
}
