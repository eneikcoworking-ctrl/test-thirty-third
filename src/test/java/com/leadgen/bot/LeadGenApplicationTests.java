package com.leadgen.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class LeadGenApplicationTests {

    @Autowired
    private TelegramAccountRepository telegramAccountRepository;

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ActiveDialogSummaryRepository activeDialogSummaryRepository;

    private TelegramAccount testAccount;

    @BeforeEach
    public void setUp() {
        // Clear everything first
        messageRepository.deleteAll();
        dialogRepository.deleteAll();
        telegramAccountRepository.deleteAll();

        // Create a test telegram account
        testAccount = new TelegramAccount();
        testAccount.setPhoneNumber("+123456789");
        testAccount.setUsername("test_operator");
        testAccount.setFirstName("Olivia");
        testAccount.setLastName("Koss");
        testAccount.setStatus("ACTIVE");
        testAccount = telegramAccountRepository.save(testAccount);
    }

    @Test
    public void contextLoads() {
        // Basic context loading check
    }

    @Test
    public void testActiveDialogSummaryViewAndOptimizedIndexes() throws Exception {
        // 1. Create multiple dialogs: 2 Active, 1 Inactive
        Dialog dialog1 = new Dialog();
        dialog1.setTelegramAccount(testAccount);
        dialog1.setPeerId("peer_100");
        dialog1.setPeerUsername("lead_one");
        dialog1.setPeerPhoneNumber("+999888111");
        dialog1.setStatus("QUALIFIED");
        dialog1.setIsActive(true);
        final Dialog savedDialog1 = dialogRepository.save(dialog1);

        Dialog dialog2 = new Dialog();
        dialog2.setTelegramAccount(testAccount);
        dialog2.setPeerId("peer_200");
        dialog2.setPeerUsername("lead_two");
        dialog2.setPeerPhoneNumber("+999888222");
        dialog2.setStatus("PENDING");
        dialog2.setIsActive(true);
        final Dialog savedDialog2 = dialogRepository.save(dialog2);

        Dialog dialog3 = new Dialog();
        dialog3.setTelegramAccount(testAccount);
        dialog3.setPeerId("peer_300");
        dialog3.setPeerUsername("lead_three");
        dialog3.setPeerPhoneNumber("+999888333");
        dialog3.setStatus("QUALIFIED");
        dialog3.setIsActive(false); // Inactive dialog
        final Dialog savedDialog3 = dialogRepository.save(dialog3);

        // 2. Add messages for dialog 1 (1 read, 2 unread, latest is unread)
        Message msg1_1 = new Message();
        msg1_1.setDialog(savedDialog1);
        msg1_1.setSenderId("peer_100");
        msg1_1.setIsFromMe(false);
        msg1_1.setIsUnread(false);
        msg1_1.setText("Hello, I am interested");
        messageRepository.save(msg1_1);
        Thread.sleep(5); // Ensure timestamp differences if needed

        Message msg1_2 = new Message();
        msg1_2.setDialog(savedDialog1);
        msg1_2.setSenderId("peer_100");
        msg1_2.setIsFromMe(false);
        msg1_2.setIsUnread(true);
        msg1_2.setText("Can we jump on a call?");
        messageRepository.save(msg1_2);
        Thread.sleep(5);

        Message msg1_3 = new Message();
        msg1_3.setDialog(savedDialog1);
        msg1_3.setSenderId("peer_100");
        msg1_3.setIsFromMe(false);
        msg1_3.setIsUnread(true);
        msg1_3.setText("Let me know the time");
        messageRepository.save(msg1_3);

        // 3. Add messages for dialog 2 (2 read, 0 unread, latest is read)
        Message msg2_1 = new Message();
        msg2_1.setDialog(savedDialog2);
        msg2_1.setSenderId("peer_200");
        msg2_1.setIsFromMe(false);
        msg2_1.setIsUnread(false);
        msg2_1.setText("Hi there");
        messageRepository.save(msg2_1);
        Thread.sleep(5);

        Message msg2_2 = new Message();
        msg2_2.setDialog(savedDialog2);
        msg2_2.setSenderId("test_operator");
        msg2_2.setIsFromMe(true);
        msg2_2.setIsUnread(false);
        msg2_2.setText("Sure, how can I help?");
        messageRepository.save(msg2_2);

        // 4. Query the ActiveDialogSummary view
        List<ActiveDialogSummary> activeSummaries = activeDialogSummaryRepository.findAll();

        // 5. Assertions
        // Active dialogs should only return active ones (dialog1 and dialog2, but not dialog3)
        assertThat(activeSummaries).hasSize(2);

        // Verify Dialog 1 Details
        ActiveDialogSummary summary1 = activeSummaries.stream()
                .filter(s -> s.getDialogId().equals(savedDialog1.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Dialog 1 summary not found in view"));

        assertThat(summary1.getPeerUsername()).isEqualTo("lead_one");
        assertThat(summary1.getStatus()).isEqualTo("QUALIFIED");
        assertThat(summary1.getUnreadCount()).isEqualTo(2); // msg1_2 and msg1_3 are unread
        assertThat(summary1.getLatestMessageText()).isEqualTo("Let me know the time");

        // Verify Dialog 2 Details
        ActiveDialogSummary summary2 = activeSummaries.stream()
                .filter(s -> s.getDialogId().equals(savedDialog2.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Dialog 2 summary not found in view"));

        assertThat(summary2.getPeerUsername()).isEqualTo("lead_two");
        assertThat(summary2.getStatus()).isEqualTo("PENDING");
        assertThat(summary2.getUnreadCount()).isEqualTo(0);
        assertThat(summary2.getLatestMessageText()).isEqualTo("Sure, how can I help?");

        // Verify fetching active dialogs grouped/filtered by status
        List<ActiveDialogSummary> qualifiedSummaries = activeDialogSummaryRepository.findByStatus("QUALIFIED");
        assertThat(qualifiedSummaries).hasSize(1);
        assertThat(qualifiedSummaries.get(0).getDialogId()).isEqualTo(savedDialog1.getId());
    }
}
