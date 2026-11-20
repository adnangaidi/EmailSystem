package com.projects.adnan.EmailProject.mail;

import com.projects.adnan.EmailProject.config.MailProperties;
import com.projects.adnan.EmailProject.entity.MailAccount;
import com.projects.adnan.EmailProject.security.EncryptionService;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class Pop3Service {

    private static final int MAX_ATTACHMENT_BYTES = 10 * 1024 * 1024;
    private static final int MAX_ATTACHMENTS = 10;
    private static final int MAX_BODY_CHARS = 1_000_000;

    private final EncryptionService encryption;
    private final MailProperties props;

    public record ParsedAttachment(String filename, String contentType, byte[] data) {}

    public record ParsedMail(String messageId, String from, String recipients, String subject,
                             String body, boolean html, LocalDateTime date,
                             List<ParsedAttachment> attachments) {}

    private static final class Body {
        final StringBuilder plain = new StringBuilder();
        final StringBuilder html = new StringBuilder();
    }

    /** Reads the last {@code max} messages (READ_ONLY: nothing is deleted on the server). */
    public List<ParsedMail> fetchLatest(MailAccount account, int max) {
        boolean ssl = props.getPop3().isSsl();
        String protocol = ssl ? "pop3s" : "pop3";

        Properties p = new Properties();
        p.put("mail." + protocol + ".connectiontimeout", "10000");
        p.put("mail." + protocol + ".timeout", "20000");
        if (ssl) {
            p.put("mail.pop3s.ssl.enable", "true");
            p.put("mail.pop3s.ssl.checkserveridentity", "true");
        }

        List<ParsedMail> result = new ArrayList<>();
        try (Store store = Session.getInstance(p).getStore(protocol)) {
            store.connect(account.getPop3Host(), account.getPop3Port(), account.getMailUsername(),
                    encryption.decryptFromBase64(account.getMailPasswordEnc()));
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            try {
                int total = inbox.getMessageCount();
                if (total == 0) return result;
                int start = Math.max(1, total - max + 1);
                for (Message m : inbox.getMessages(start, total)) {
                    try {
                        result.add(parse((MimeMessage) m));   // parse before the folder closes
                    } catch (Exception e) {
                        log.warn("Skipping unreadable message #{}: {}", m.getMessageNumber(), e.getMessage());
                    }
                }
            } finally {
                if (inbox.isOpen()) inbox.close(false);
            }
        } catch (MessagingException e) {
            log.warn("POP3 error for {}: {}", account.getMailUsername(), e.getMessage());
            throw new IllegalStateException("Could not connect to the mail server.", e);
        }
        return result;
    }

    // ---------------------------------------------------------------- MIME parsing

    private ParsedMail parse(MimeMessage m) throws Exception {
        String from = join(m.getFrom());
        String recipients = join(m.getAllRecipients());
        String subject = m.getSubject() == null ? "(no subject)" : m.getSubject();
        Date sent = m.getSentDate();
        LocalDateTime date = sent == null ? LocalDateTime.now()
                : LocalDateTime.ofInstant(sent.toInstant(), ZoneId.systemDefault());

        String messageId = m.getMessageID();
        if (messageId == null || messageId.isBlank()) {
            messageId = "<generated-" + sha256(from + date + subject) + "@local>";
        }

        Body body = new Body();
        List<ParsedAttachment> attachments = new ArrayList<>();
        walk(m, body, attachments);

        boolean html = body.html.length() > 0;
        String text = html ? body.html.toString() : body.plain.toString();
        return new ParsedMail(messageId, from, recipients, subject, text, html, date, attachments);
    }

    private void walk(Part part, Body body, List<ParsedAttachment> attachments) throws Exception {
        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                walk(mp.getBodyPart(i), body, attachments);
            }
            return;
        }

        String filename = part.getFileName();
        boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || filename != null;

        if (!isAttachment && part.isMimeType("text/plain")) {
            append(body.plain, part.getContent());
        } else if (!isAttachment && part.isMimeType("text/html")) {
            append(body.html, part.getContent());
        } else if (isAttachment && attachments.size() < MAX_ATTACHMENTS) {
            try (InputStream in = part.getInputStream()) {
                byte[] data = in.readNBytes(MAX_ATTACHMENT_BYTES + 1);
                if (data.length > MAX_ATTACHMENT_BYTES) {
                    log.info("Attachment skipped (larger than {} bytes)", MAX_ATTACHMENT_BYTES);
                    return;
                }
                String type = part.getContentType() == null ? "application/octet-stream"
                        : part.getContentType().split(";")[0].trim();
                attachments.add(new ParsedAttachment(decodeName(filename), type, data));
            }
        }
    }

    private void append(StringBuilder sb, Object content) {
        if (content instanceof String s && sb.length() < MAX_BODY_CHARS) {
            sb.append(s);
        }
    }

    private String decodeName(String name) {
        if (name == null) return "attachment";
        try {
            return MimeUtility.decodeText(name);
        } catch (Exception e) {
            return name;
        }
    }

    private String join(Address[] addresses) {
        if (addresses == null) return "";
        return Arrays.stream(addresses)
                .map(a -> a instanceof InternetAddress ia ? ia.toUnicodeString() : a.toString())
                .collect(Collectors.joining(", "));
    }

    private String sha256(String s) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash).substring(0, 40);
    }
}
