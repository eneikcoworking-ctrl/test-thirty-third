package com.leadgen.bot.service;

import com.leadgen.bot.model.Campaign;
import com.leadgen.bot.model.Contact;
import com.leadgen.bot.model.DispatchedMessage;
import com.leadgen.bot.repository.CampaignRepository;
import com.leadgen.bot.repository.ContactRepository;
import com.leadgen.bot.repository.DispatchedMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CampaignAndMessagePersistenceTest {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private MessageLogService messageLogService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private DispatchedMessageRepository dispatchedMessageRepository;

    @Test
    @Transactional
    public void testCreateCampaignAndLinkToContactsAndSpintax() {
        // Given a new campaign
        String name = "Q3 Outreach";
        String spintaxTemplate = "Hello {friend|there}, {check this out|have a look}!";
        List<String> contactInputs = List.of("@john_doe", "+12345678901", "invalid_format_default_username");

        // When saved
        Campaign campaign = campaignService.createCampaign(name, spintaxTemplate, contactInputs);

        // Then it is linked to the uploaded contact list and spintax template
        assertThat(campaign.getId()).isNotNull();
        assertThat(campaign.getName()).isEqualTo(name);
        assertThat(campaign.getSpintaxTemplate()).isEqualTo(spintaxTemplate);

        // Retrieve and check
        Campaign retrieved = campaignService.getCampaign(campaign.getId());
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getContacts()).hasSize(3);

        // Verify contact details
        Contact contact1 = retrieved.getContacts().stream()
                .filter(c -> "@john_doe".equals(c.getUsername()))
                .findFirst()
                .orElse(null);
        assertThat(contact1).isNotNull();
        assertThat(contact1.getCampaign().getId()).isEqualTo(campaign.getId());

        Contact contact2 = retrieved.getContacts().stream()
                .filter(c -> "+12345678901".equals(c.getPhoneNumber()))
                .findFirst()
                .orElse(null);
        assertThat(contact2).isNotNull();
        assertThat(contact2.getCampaign().getId()).isEqualTo(campaign.getId());
    }

    @Test
    @Transactional
    public void testLogDispatchedMessageRecordsAccountAndStatus() {
        // Prepare campaign and contact
        Campaign campaign = campaignService.createCampaign(
                "Delivery Test",
                "Hi!",
                List.of("@target_user")
        );
        Contact contact = campaign.getContacts().get(0);

        // Given a dispatched message, When logged
        String assignedAccount = "acc_session_1";
        String deliveryStatus = "DELIVERED";
        DispatchedMessage loggedMessage = messageLogService.logMessage(campaign, contact, assignedAccount, deliveryStatus);

        // Then it records the assigned account and delivery status
        assertThat(loggedMessage.getId()).isNotNull();
        assertThat(loggedMessage.getCampaign().getId()).isEqualTo(campaign.getId());
        assertThat(loggedMessage.getContact().getId()).isEqualTo(contact.getId());
        assertThat(loggedMessage.getAssignedAccount()).isEqualTo(assignedAccount);
        assertThat(loggedMessage.getDeliveryStatus()).isEqualTo(deliveryStatus);

        // Verify in DB
        List<DispatchedMessage> messages = dispatchedMessageRepository.findByCampaignId(campaign.getId());
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getAssignedAccount()).isEqualTo(assignedAccount);
        assertThat(messages.get(0).getDeliveryStatus()).isEqualTo(deliveryStatus);
    }
}
