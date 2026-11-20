package com.projects.adnan.EmailProject.mail;
import com.projects.adnan.EmailProject.config.MailProperties;
import com.projects.adnan.EmailProject.entity.MailAccount;
import com.projects.adnan.EmailProject.security.EncryptionService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SmtpService {

    private final EncryptionService encryption;
    private final MailProperties props;

    public record OutgoingAttachment(String filename, String contentType, byte[] data) {}

    public record OutgoingMail(List<String> to, List<String> cc, String subject, String body,
                               List<OutgoingAttachment> attachments) {}

    /** Sends the mail through the user's own SMTP account. Returns the Message-ID. */
    public String send(MailAccount account, OutgoingMail mail) {
        JavaMailSenderImpl sender = buildSender(account);
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(account.getMailUsername());
            helper.setTo(mail.to().toArray(String[]::new));
            if (!mail.cc().isEmpty()) {
                helper.setCc(mail.cc().toArray(String[]::new));
            }
            helper.setSubject(mail.subject());
            helper.setText(mail.body(), false);
            for (OutgoingAttachment a : mail.attachments()) {
                helper.addAttachment(a.filename(), new ByteArrayResource(a.data()), a.contentType());
            }
            sender.send(message);                       // throws MailException on SMTP failure
            String id = message.getMessageID();
            return id != null ? id : "<" + UUID.randomUUID() + "@local>";
        } catch (MessagingException e) {
            throw new IllegalStateException("Could not build the email message.", e);
        }
    }

    private JavaMailSenderImpl buildSender(MailAccount account) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(account.getSmtpHost());
        sender.setPort(account.getSmtpPort());
        sender.setUsername(account.getMailUsername());
        sender.setPassword(encryption.decryptFromBase64(account.getMailPasswordEnc()));
        sender.setDefaultEncoding("UTF-8");

        Properties p = sender.getJavaMailProperties();
        p.put("mail.transport.protocol", "smtp");
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.connectiontimeout", "10000");
        p.put("mail.smtp.timeout", "20000");
        p.put("mail.smtp.writetimeout", "20000");
        if (account.getSmtpPort() == 465) {                    // implicit TLS
            p.put("mail.smtp.ssl.enable", "true");
            p.put("mail.smtp.ssl.checkserveridentity", "true");
        } else if (props.getSmtp().isStarttls()) {             // STARTTLS (587)
            p.put("mail.smtp.starttls.enable", "true");
            p.put("mail.smtp.starttls.required", "true");
            p.put("mail.smtp.ssl.checkserveridentity", "true");
        }
        return sender;
    }
}
