package com.leadgen.bot.controller;

import com.leadgen.bot.dto.*;
import com.leadgen.bot.model.Campaign;
import com.leadgen.bot.model.CampaignExecutionResult;
import com.leadgen.bot.repository.DispatchedMessageRepository;
import com.leadgen.bot.service.CampaignOrchestrationService;
import com.leadgen.bot.service.CampaignService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;
    private final CampaignOrchestrationService campaignOrchestrationService;
    private final DispatchedMessageRepository dispatchedMessageRepository;

    public CampaignController(
            CampaignService campaignService,
            CampaignOrchestrationService campaignOrchestrationService,
            DispatchedMessageRepository dispatchedMessageRepository) {
        this.campaignService = campaignService;
        this.campaignOrchestrationService = campaignOrchestrationService;
        this.dispatchedMessageRepository = dispatchedMessageRepository;
    }

    @GetMapping
    public ResponseEntity<List<CampaignSummaryResponse>> listCampaigns() {
        List<Campaign> campaigns = campaignService.getAllCampaigns();
        List<CampaignSummaryResponse> response = campaigns.stream().map(c -> {
            CampaignSummaryResponse summary = new CampaignSummaryResponse();
            summary.setId(c.getId());
            summary.setName(c.getName());
            summary.setSpintaxTemplate(c.getSpintaxTemplate());
            summary.setCreatedAt(c.getCreatedAt());

            int total = c.getContacts() != null ? c.getContacts().size() : 0;
            int pending = c.getContacts() != null ? (int) c.getContacts().stream()
                    .filter(contact -> "PENDING".equalsIgnoreCase(contact.getStatus()))
                    .count() : 0;
            int dispatched = (int) dispatchedMessageRepository.findByCampaignId(c.getId()).size();

            summary.setTotalContactsCount(total);
            summary.setPendingContactsCount(pending);
            summary.setDispatchedMessagesCount(dispatched);
            return summary;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CampaignDetailResponse> createCampaign(@RequestBody CampaignCreateRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty() ||
            request.getSpintaxTemplate() == null || request.getSpintaxTemplate().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Campaign campaign = campaignService.createCampaign(request.getName(), request.getSpintaxTemplate(), new ArrayList<>());
        CampaignDetailResponse response = mapToDetailResponse(campaign);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignDetailResponse> getCampaign(@PathVariable Long id) {
        Campaign campaign = campaignService.getCampaign(id);
        if (campaign == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToDetailResponse(campaign));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignDetailResponse> updateCampaign(@PathVariable Long id, @RequestBody CampaignUpdateRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty() ||
            request.getSpintaxTemplate() == null || request.getSpintaxTemplate().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Campaign campaign = campaignService.updateCampaign(id, request.getName(), request.getSpintaxTemplate());
        if (campaign == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToDetailResponse(campaign));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {
        Campaign campaign = campaignService.getCampaign(id);
        if (campaign == null) {
            return ResponseEntity.notFound().build();
        }
        campaignService.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<CampaignExecuteResponse> executeCampaign(@PathVariable Long id, @RequestBody CampaignExecuteRequest request) {
        Campaign campaign = campaignService.getCampaign(id);
        if (campaign == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.getAccountIds() == null) {
            return ResponseEntity.badRequest().build();
        }

        CampaignExecutionResult result = campaignOrchestrationService.executeCampaign(id, request.getAccountIds());

        CampaignExecuteResponse response = new CampaignExecuteResponse();
        response.setCampaignId(result.getCampaignId());
        response.setStatus(result.getStatus());
        response.setMessagesSent(result.getMessagesSent());
        response.setRotationsCount(result.getRotationsCount());
        response.setLogs(result.getLogs());

        return ResponseEntity.ok(response);
    }

    private CampaignDetailResponse mapToDetailResponse(Campaign campaign) {
        CampaignDetailResponse detail = new CampaignDetailResponse();
        detail.setId(campaign.getId());
        detail.setName(campaign.getName());
        detail.setSpintaxTemplate(campaign.getSpintaxTemplate());
        detail.setCreatedAt(campaign.getCreatedAt());

        CampaignStatsResponse stats = new CampaignStatsResponse();
        int total = campaign.getContacts() != null ? campaign.getContacts().size() : 0;
        int pending = campaign.getContacts() != null ? (int) campaign.getContacts().stream()
                .filter(c -> "PENDING".equalsIgnoreCase(c.getStatus()))
                .count() : 0;
        int invalid = campaign.getContacts() != null ? (int) campaign.getContacts().stream()
                .filter(c -> "INVALID".equalsIgnoreCase(c.getStatus()))
                .count() : 0;

        List<com.leadgen.bot.model.DispatchedMessage> messages = dispatchedMessageRepository.findByCampaignId(campaign.getId());
        int delivered = (int) messages.stream().filter(m -> "DELIVERED".equalsIgnoreCase(m.getDeliveryStatus())).count();
        int failed = (int) messages.stream().filter(m -> "FAILED".equalsIgnoreCase(m.getDeliveryStatus())).count();

        stats.setTotalContacts(total);
        stats.setPendingContacts(pending);
        stats.setInvalidContacts(invalid);
        stats.setDeliveredCount(delivered);
        stats.setFailedCount(failed);

        detail.setStats(stats);
        return detail;
    }
}
