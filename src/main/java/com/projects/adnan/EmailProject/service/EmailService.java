package com.projects.adnan.EmailProject.service;

import com.projects.adnan.EmailProject.dto.ComposeForm;
import com.projects.adnan.EmailProject.dto.EmailView;
import com.projects.adnan.EmailProject.entity.Attachment;
import com.projects.adnan.EmailProject.entity.EmailMessage;
import com.projects.adnan.EmailProject.entity.MailAccount;
import com.projects.adnan.EmailProject.entity.User;
import com.projects.adnan.EmailProject.entity.enums.Folder;
import com.projects.adnan.EmailProject.exception.ResourceNotFoundException;
import com.projects.adnan.EmailProject.mail.Pop3Service;
import com.projects.adnan.EmailProject.mail.SmtpService;
import com.projects.adnan.EmailProject.repository.AttachmentRepository;
import com.projects.adnan.EmailProject.repository.EmailMessageRepository;
import com.projects.adnan.EmailProject.repository.MailAccountRepository;
import com.projects.adnan.EmailProject.repository.UserRepository;
import com.projects.adnan.EmailProject.security.EncryptionService;
import com.projects.adnan.EmailProject.util.InputSanitizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final int PAGE_SIZE = 20;
    private static final int SYNC_BATCH = 100;
    private static final int MAX_FILES = 5;
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    private final EmailMessageRepository emails;
    private final AttachmentRepository attachments;
    private final UserRepository users;
    private final MailAccountRepository accounts;
    private final SmtpService smtp;
    private final Pop3Service pop3;
    private final EncryptionService crypto;

    public record AttachmentContent(String filename, String contentType, byte[] data) {}

    // ------------------------------------------------------------------ compose / send

    @Transactional
    public void send(Long userId, @Valid ComposeForm form) {
        User user = requireUser(userId);
        MailAccount account = requireAccount(userId);

        List<String> to = InputSanitizer.parseAddresses(form.getTo());
        List<String> cc = InputSanitizer.parseAddresses(form.getCc());
        if (to.isEmpty()) throw new IllegalArgumentException("At least one recipient is required.");
        String subject = InputSanitizer.cleanHeader(form.getSubject());
        List<SmtpService.OutgoingAttachment> files = readAttachments(form.getAttachments());

        String messageId = smtp.send(account, new SmtpService.OutgoingMail(to, cc, subject, form.getBody(), files));

        // keep a copy in the Sent folder (encrypted)
        EmailMessage copy = new EmailMessage();
        copy.setUser(user);
        copy.setMessageId(cut(messageId, 255));
        copy.setFolder(Folder.SENT);
        copy.setSender(cut(account.getMailUsername(), 255));
        List<String> all = new ArrayList<>(to);
        all.addAll(cc);
        copy.setRecipients(String.join(", ", all));
        copy.setSubject(cut(subject, 500));
        encryptBody(copy, InputSanitizer.plainToHtml(form.getBody()));
        copy.setSentAt(LocalDateTime.now());
        copy.setRead(true);
        for (SmtpService.OutgoingAttachment f : files) {
            copy.addAttachment(buildAttachment(f.filename(), f.contentType(), f.data()));
        }
        emails.save(copy);
    }

    // ------------------------------------------------------------------ receive (POP3)

    /** Fetches new mail from POP3 and stores it encrypted. Returns the number of new messages. */
    @Transactional
    public int syncInbox(Long userId) {
        User user = requireUser(userId);
        MailAccount account = requireAccount(userId);

        int added = 0;
        for (Pop3Service.ParsedMail m : pop3.fetchLatest(account, SYNC_BATCH)) {
            String messageId = cut(m.messageId(), 255);
            if (emails.existsByUserIdAndMessageIdAndFolderIn(userId, messageId, List.of(Folder.INBOX, Folder.TRASH))) {
                continue;                                  // already imported (or deleted by the user)
            }
            EmailMessage e = new EmailMessage();
            e.setUser(user);
            e.setMessageId(messageId);
            e.setFolder(Folder.INBOX);
            e.setSender(cut(m.from(), 255));
            e.setRecipients(m.recipients());
            e.setSubject(cut(m.subject(), 500));
            encryptBody(e, m.html() ? InputSanitizer.sanitizeHtml(m.body()) : InputSanitizer.plainToHtml(m.body()));
            e.setSentAt(m.date());
            e.setReceivedAt(LocalDateTime.now());
            e.setRead(false);

            for (Pop3Service.ParsedAttachment a : m.attachments()) {
                String name = InputSanitizer.safeFilename(a.filename());
                if (InputSanitizer.isBlockedExtension(name)) continue;
                e.addAttachment(buildAttachment(name, a.contentType(), a.data()));
            }
            emails.save(e);
            added++;
        }
        return added;
    }

    // ------------------------------------------------------------------ read / history

    @Transactional(readOnly = true)
    public Page<EmailView> list(Long userId, Folder folder, int page) {
        Sort sort = Sort.by(Sort.Direction.DESC, "sentAt").and(Sort.by(Sort.Direction.DESC, "id"));
        return emails.findByUserIdAndFolder(userId, folder, PageRequest.of(page, PAGE_SIZE, sort))
                .map(e -> toView(e, false));
    }

    @Transactional
    public EmailView get(Long userId, Long id) {
        EmailMessage e = emails.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found."));
        if (!e.isRead()) e.setRead(true);
        return toView(e, true);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return emails.countByUserIdAndFolderAndReadFalse(userId, Folder.INBOX);
    }

    /** First delete moves to Trash, deleting from Trash removes it permanently. */
    @Transactional
    public void delete(Long userId, Long id) {
        EmailMessage e = emails.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found."));
        if (e.getFolder() == Folder.TRASH) {
            emails.delete(e);
        } else {
            e.setFolder(Folder.TRASH);
        }
    }

    @Transactional(readOnly = true)
    public AttachmentContent getAttachment(Long userId, Long attachmentId) {
        Attachment a = attachments.findByIdAndEmailUserId(attachmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found."));
        return new AttachmentContent(a.getFilename(), a.getContentType(), crypto.decrypt(a.getIv(), a.getDataEnc()));
    }

    // ------------------------------------------------------------------ helpers

    private EmailView toView(EmailMessage e, boolean full) {
        String body = null;
        List<EmailView.AttachmentInfo> files = List.of();
        if (full) {
            // sanitized again at read time (defense in depth) before th:utext
            body = e.getBodyEnc() == null ? ""
                    : InputSanitizer.sanitizeHtml(crypto.decryptString(e.getIv(), e.getBodyEnc()));
            files = e.getAttachments().stream()
                    .map(a -> new EmailView.AttachmentInfo(a.getId(), a.getFilename(), a.getSize()))
                    .toList();
        }
        return new EmailView(e.getId(), e.getFolder(), e.getSender(), e.getRecipients(),
                e.getSubject(), body, e.getSentAt(), e.isRead(), files);
    }

    private void encryptBody(EmailMessage e, String html) {
        EncryptionService.Encrypted enc = crypto.encryptString(html);
        e.setBodyEnc(enc.data());
        e.setIv(enc.iv());
    }

    private Attachment buildAttachment(String filename, String contentType, byte[] data) {
        EncryptionService.Encrypted enc = crypto.encrypt(data);
        Attachment a = new Attachment();
        a.setFilename(InputSanitizer.safeFilename(filename));
        a.setContentType(contentType == null ? "application/octet-stream" : cut(contentType, 150));
        a.setSize(data.length);
        a.setDataEnc(enc.data());
        a.setIv(enc.iv());
        return a;
    }

    private List<SmtpService.OutgoingAttachment> readAttachments(List<MultipartFile> files) {
        List<SmtpService.OutgoingAttachment> out = new ArrayList<>();
        if (files == null) return out;
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;
            if (out.size() >= MAX_FILES) throw new IllegalArgumentException("Too many attachments (max " + MAX_FILES + ").");
            if (f.getSize() > MAX_FILE_BYTES) throw new IllegalArgumentException("Attachment too large (max 10 MB).");
            String name = InputSanitizer.safeFilename(f.getOriginalFilename());
            if (InputSanitizer.isBlockedExtension(name)) {
                throw new IllegalArgumentException("File type not allowed: " + name);
            }
            try {
                String type = f.getContentType() == null ? "application/octet-stream" : f.getContentType();
                out.add(new SmtpService.OutgoingAttachment(name, type, f.getBytes()));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        return out;
    }

    private User requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private MailAccount requireAccount(Long userId) {
        return accounts.findByUserId(userId).orElseThrow(() ->
                new IllegalStateException("No mailbox is configured for this account. Ask an administrator."));
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
