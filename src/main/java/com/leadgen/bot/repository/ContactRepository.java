package com.leadgen.bot.repository;

import com.leadgen.bot.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByCampaignId(Long campaignId);
}
