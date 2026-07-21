package com.leadgen.bot.service;

import com.leadgen.bot.model.Campaign;
import com.leadgen.bot.model.Contact;
import com.leadgen.bot.model.DispatchedMessage;
import com.leadgen.bot.repository.DispatchedMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageLogService {

    private final DispatchedMessageRepository dispatchedMessageRepository;

    public MessageLogService(DispatchedMessageRepository dispatchedMessageRepository) {
        this.dispatchedMessageRepository = dispatchedMessageRepository;
    }

    @Transactional
    public DispatchedMessage logMessage(Campaign campaign, Contact contact, String assignedAccount, String deliveryStatus) {
        DispatchedMessage message = new DispatchedMessage(campaign, contact, assignedAccount, deliveryStatus);
        return dispatchedMessageRepository.save(message);
    }
}
