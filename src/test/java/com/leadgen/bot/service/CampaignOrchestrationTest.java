package com.leadgen.bot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadgen.bot.TelegramAccount;
import com.leadgen.bot.TelegramAccountRepository;
import com.leadgen.bot.dto.CampaignCreateRequest;
import com.leadgen.bot.dto.CampaignExecuteRequest;
import com.leadgen.bot.dto.CampaignUpdateRequest;
import com.leadgen.bot.model.Campaign;
import com.leadgen.bot.model.CampaignExecutionResult;
import com.leadgen.bot.model.Contact;
import com.leadgen.bot.model.DispatchedMessage;
import com.leadgen.bot.repository.CampaignRepository;
import com.leadgen.bot.repository.ContactRepository;
import com.leadgen.bot.repository.DispatchedMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CampaignOrchestrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private CampaignOrchestrationService campaignOrchestrationService;

    @Autowired
    private TelegramService telegramService;

    @Autowired
    private TelegramAccountRepository telegramAccountRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private DispatchedMessageRepository dispatchedMessageRepository;

    @BeforeEach
    public void setUp() {
        telegramService.clearBehaviors();
        dispatchedMessageRepository.deleteAll();
        contactRepository.deleteAll();
        campaignRepository.deleteAll();
        telegramAccountRepository.deleteAll();
    }

    @Test
    public void testSpintaxParserFlatAndNested() {
        // Test flat spintax
        String template1 = "Hello {friend|there}!";
        Set<String> results1 = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            results1.add(SpintaxParser.parse(template1));
        }
        assertThat(results1).containsExactlyInAnyOrder("Hello friend!", "Hello there!");

        // Test nested spintax
        String template2 = "{Good morning|Hi {buddy|partner}}, look here!";
        Set<String> results2 = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            results2.add(SpintaxParser.parse(template2));
        }
        assertThat(results2).containsExactlyInAnyOrder(
                "Good morning, look here!",
                "Hi buddy, look here!",
                "Hi partner, look here!"
        );
    }

    @Test
    public void testCampaignExecutionWithAccountRotationAndFloodLimits() {
        // 1. Create a campaign with 3 pending contacts
        Campaign campaign = campaignService.createCampaign(
                "Automation Test Campaign",
                "Hi, {check this out|exclusive deal}!",
                List.of("@lead1", "@lead2", "@lead3")
        );

        // 2. Create 3 Telegram accounts
        TelegramAccount acc1 = new TelegramAccount();
        acc1.setUsername("acc_flood");
        acc1.setStatus("ACTIVE");
        acc1 = telegramAccountRepository.save(acc1);

        TelegramAccount acc2 = new TelegramAccount();
        acc2.setUsername("acc_limit");
        acc2.setStatus("ACTIVE");
        acc2 = telegramAccountRepository.save(acc2);

        TelegramAccount acc3 = new TelegramAccount();
        acc3.setUsername("acc_success");
        acc3.setStatus("ACTIVE");
        acc3 = telegramAccountRepository.save(acc3);

        // Configure mock behaviors for these accounts
        telegramService.setBehavior(acc1.getId(), "FLOOD");
        telegramService.setBehavior(acc2.getId(), "LIMIT");
        // acc3 has default (SUCCESS) behavior

        // 3. Execute campaign with rotation
        CampaignExecutionResult result = campaignOrchestrationService.executeCampaign(
                campaign.getId(),
                List.of(acc1.getId(), acc2.getId(), acc3.getId())
        );

        // 4. Assert orchestration behavior
        // Execution must complete successfully overall because acc3 is active and works
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        // It successfully sent messages to all 3 contacts
        assertThat(result.getMessagesSent()).isEqualTo(3);
        // We had 2 rotation events: acc1 hit flood, acc2 hit limit, rotating to acc3
        assertThat(result.getRotationsCount()).isEqualTo(2);

        // Verify account statuses are updated in DB
        TelegramAccount updatedAcc1 = telegramAccountRepository.findById(acc1.getId()).orElseThrow();
        assertThat(updatedAcc1.getStatus()).isEqualTo("FLOOD_LIMIT"); // Paused/flood limited

        TelegramAccount updatedAcc2 = telegramAccountRepository.findById(acc2.getId()).orElseThrow();
        assertThat(updatedAcc2.getStatus()).isEqualTo("LIMIT_REACHED"); // Paused/daily limit reached

        TelegramAccount updatedAcc3 = telegramAccountRepository.findById(acc3.getId()).orElseThrow();
        assertThat(updatedAcc3.getStatus()).isEqualTo("ACTIVE"); // Still active

        // Verify messages logged in database
        List<DispatchedMessage> loggedMessages = dispatchedMessageRepository.findByCampaignId(campaign.getId());
        assertThat(loggedMessages).hasSize(3);

        // All successful messages should have been dispatched using the successful account (acc3)
        for (DispatchedMessage msg : loggedMessages) {
            assertThat(msg.getAssignedAccount()).isEqualTo("acc_success");
            assertThat(msg.getDeliveryStatus()).isEqualTo("DELIVERED");
        }

        // Verify all campaign contacts status changed to COMPLETED
        Campaign updatedCampaign = campaignService.getCampaign(campaign.getId());
        for (Contact contact : updatedCampaign.getContacts()) {
            assertThat(contact.getStatus()).isEqualTo("COMPLETED");
        }
    }

    @Test
    public void testCampaignExecutionWithNoAvailableAccounts() {
        Campaign campaign = campaignService.createCampaign(
                "No Accs",
                "Hello",
                List.of("@lead")
        );

        TelegramAccount acc = new TelegramAccount();
        acc.setUsername("acc_inactive");
        acc.setStatus("FLOOD_LIMIT"); // Inactive
        acc = telegramAccountRepository.save(acc);

        CampaignExecutionResult result = campaignOrchestrationService.executeCampaign(
                campaign.getId(),
                List.of(acc.getId())
        );

        assertThat(result.getStatus()).isEqualTo("SUSPENDED");
        assertThat(result.getMessagesSent()).isEqualTo(0);
        assertThat(result.getLogs().get(0)).contains("No active Telegram accounts");
    }

    @Test
    public void testCampaignControllerEndpoints() throws Exception {
        // 1. Create campaign via REST API
        CampaignCreateRequest createReq = new CampaignCreateRequest();
        createReq.setName("REST Campaign");
        createReq.setSpintaxTemplate("Hey {there|buddy}!");

        String responseJson = mockMvc.perform(post("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("REST Campaign"))
                .andExpect(jsonPath("$.spintaxTemplate").value("Hey {there|buddy}!"))
                .andReturn().getResponse().getContentAsString();

        Long campaignId = objectMapper.readTree(responseJson).get("id").asLong();

        // Add a contact to the campaign using repository so we can run execution
        Campaign campaign = campaignRepository.findById(campaignId).orElseThrow();
        Contact contact = new Contact();
        contact.setUsername("@rest_lead");
        contact.setStatus("PENDING");
        campaign.addContact(contact);
        campaignRepository.save(campaign);

        // 2. List campaigns
        mockMvc.perform(get("/api/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(campaignId))
                .andExpect(jsonPath("$[0].name").value("REST Campaign"))
                .andExpect(jsonPath("$[0].totalContactsCount").value(1))
                .andExpect(jsonPath("$[0].pendingContactsCount").value(1));

        // 3. Get campaign details
        mockMvc.perform(get("/api/campaigns/" + campaignId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(campaignId))
                .andExpect(jsonPath("$.stats.totalContacts").value(1))
                .andExpect(jsonPath("$.stats.pendingContacts").value(1));

        // Create an active telegram account for execution
        TelegramAccount activeAcc = new TelegramAccount();
        activeAcc.setUsername("rest_acc");
        activeAcc.setStatus("ACTIVE");
        activeAcc = telegramAccountRepository.save(activeAcc);

        // 4. Execute campaign endpoint
        CampaignExecuteRequest execReq = new CampaignExecuteRequest();
        execReq.setAccountIds(List.of(activeAcc.getId()));

        mockMvc.perform(post("/api/campaigns/" + campaignId + "/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(execReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value(campaignId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.messagesSent").value(1))
                .andExpect(jsonPath("$.rotationsCount").value(0));

        // 5. Update campaign
        CampaignUpdateRequest updateReq = new CampaignUpdateRequest();
        updateReq.setName("Updated REST Campaign");
        updateReq.setSpintaxTemplate("Hi {there|buddy}!");

        mockMvc.perform(put("/api/campaigns/" + campaignId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated REST Campaign"));

        // 6. Delete campaign
        mockMvc.perform(delete("/api/campaigns/" + campaignId))
                .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/api/campaigns/" + campaignId))
                .andExpect(status().isNotFound());
    }
}
