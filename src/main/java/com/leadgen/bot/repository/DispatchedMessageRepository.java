package com.leadgen.bot.repository;

import com.leadgen.bot.model.DispatchedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispatchedMessageRepository extends JpaRepository<DispatchedMessage, Long> {
    List<DispatchedMessage> findByCampaignId(Long campaignId);
}
