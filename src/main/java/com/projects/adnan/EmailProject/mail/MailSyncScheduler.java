package com.projects.adnan.EmailProject.mail;

import com.projects.adnan.EmailProject.repository.MailAccountRepository;
import com.projects.adnan.EmailProject.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically fetches new mail (POP3) for every enabled, unlocked account. */
@Component
@EnableScheduling
@Slf4j
//@RequiredArgsConstructor
public class MailSyncScheduler {

    private final MailAccountRepository accounts;
    private final EmailService emailService;
    public MailSyncScheduler(MailAccountRepository accounts, EmailService emailService) {
        this.accounts = accounts;
        this.emailService = emailService;
    }

    @Scheduled(fixedDelayString = "${app.mail.sync-interval-ms:300000}",
            initialDelayString = "${app.mail.sync-initial-delay-ms:30000}")
    public void syncAll() {
        for (Long userId : accounts.findSyncableUserIds()) {
            try {
                int added = emailService.syncInbox(userId);
                if (added > 0) log.info("User {}: {} new message(s)", userId, added);
            } catch (Exception e) {
                log.warn("Sync failed for user {}: {}", userId, e.getMessage());   // one failure must not stop the others
            }
        }
    }
}
