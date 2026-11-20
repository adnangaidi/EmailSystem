package com.projects.adnan.EmailProject.service;

import com.projects.adnan.EmailProject.dto.MailAccountForm;
import com.projects.adnan.EmailProject.dto.UserForm;
import com.projects.adnan.EmailProject.entity.MailAccount;
import com.projects.adnan.EmailProject.entity.User;
import com.projects.adnan.EmailProject.entity.enums.Folder;
import com.projects.adnan.EmailProject.entity.enums.Role;
import com.projects.adnan.EmailProject.exception.ResourceNotFoundException;
import com.projects.adnan.EmailProject.repository.EmailMessageRepository;
import com.projects.adnan.EmailProject.repository.MailAccountRepository;
import com.projects.adnan.EmailProject.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Locale;

/** Account management. Works on accounts and mail metadata only, never on email content. */
@Slf4j
@Service
//@RequiredArgsConstructor
public class AdminService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private final SecureRandom random = new SecureRandom();

    private final UserRepository users;
    private final MailAccountRepository accounts;
    private final EmailMessageRepository emails;
    private final PasswordEncoder encoder;
    private final UserService userService;
    public AdminService(UserRepository users, MailAccountRepository accounts, EmailMessageRepository emails, PasswordEncoder encoder, UserService userService) {
        this.users = users;
        this.accounts = accounts;
        this.emails = emails;
        this.encoder = encoder;
        this.userService = userService;
    }

    @Value
    public static class AdminStats {
        long totalUsers;
        long admins;
        long locked;
        long disabled;
        long inboxTotal;
        long sentTotal;
        long sentToday;
        long receivedToday;
    }

    @Transactional(readOnly = true)
    public Page<User> list(String q, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), 15, Sort.by(Sort.Direction.DESC, "id"));
        return q == null || q.isBlank() ? users.findAll(pageable) : users.search(q.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public AdminStats stats() {
        var startOfDay = LocalDate.now().atStartOfDay();
        return new AdminStats(
                users.count(), users.countByRole(Role.ADMIN), users.countByLockedTrue(), users.countByEnabledFalse(),
                emails.countByFolder(Folder.INBOX), emails.countByFolder(Folder.SENT),
                emails.countSince(Folder.SENT, startOfDay), emails.countSince(Folder.INBOX, startOfDay));
    }

    @Transactional
    public User create(@Valid UserForm f) {
        if (f.getPassword() == null || f.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        UserService.checkPassword(f.getPassword());
        String username = f.getUsername().trim();
        String email = f.getEmail().trim().toLowerCase(Locale.ROOT);
        assertUnique(username, email, null);

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(f.getPassword()));
        u.setRole(f.getRole());
        u.setEnabled(f.isEnabled());
        u.setLocked(f.isLocked());
        users.save(u);

        userService.saveMailAccount(u, f.getMail());
        return u;
    }

    @Transactional(readOnly = true)
    public UserForm toForm(Long id) {
        User u = require(id);
        UserForm f = new UserForm();
        f.setId(u.getId());
        f.setUsername(u.getUsername());
        f.setEmail(u.getEmail());
        f.setRole(u.getRole());
        f.setEnabled(u.isEnabled());
        f.setLocked(u.isLocked());

        MailAccountForm m = new MailAccountForm();
        accounts.findByUserId(id).ifPresentOrElse(a -> {   // never expose the password
            m.setMailUsername(a.getMailUsername());
            m.setSmtpHost(a.getSmtpHost());
            m.setSmtpPort(a.getSmtpPort());
            m.setPop3Host(a.getPop3Host());
            m.setPop3Port(a.getPop3Port());
        }, () -> {
            m.setMailUsername(u.getEmail());
        });
        f.setMail(m);
        return f;
    }

    @Transactional
    public void update(Long id, UserForm f, String currentAdmin) {
        User u = require(id);
        String username = f.getUsername().trim();
        String email = f.getEmail().trim().toLowerCase(Locale.ROOT);
        assertUnique(username, email, id);

        if (isSelf(u, currentAdmin) && (f.getRole() != Role.ADMIN || !f.isEnabled() || f.isLocked())) {
            throw new IllegalArgumentException("You cannot demote, disable or lock your own account.");
        }

        u.setUsername(username);
        u.setEmail(email);
        u.setRole(f.getRole());
        u.setEnabled(f.isEnabled());
        u.setLocked(f.isLocked());
        if (f.getPassword() != null && !f.getPassword().isBlank()) {
            UserService.checkPassword(f.getPassword());
            u.setPasswordHash(encoder.encode(f.getPassword()));
        }
        userService.saveMailAccount(u, f.getMail());
    }

    @Transactional
    public void setLocked(Long id, boolean locked, String currentAdmin) {
        User u = require(id);
        if (locked && isSelf(u, currentAdmin)) throw new IllegalArgumentException("You cannot lock your own account.");
        u.setLocked(locked);
    }

    @Transactional
    public void setEnabled(Long id, boolean enabled, String currentAdmin) {
        User u = require(id);
        if (!enabled && isSelf(u, currentAdmin)) throw new IllegalArgumentException("You cannot disable your own account.");
        u.setEnabled(enabled);
    }

    /** Sets a random temporary password and returns it (shown once to the admin). */
    @Transactional
    public String resetPassword(Long id) {
        User u = require(id);
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        u.setPasswordHash(encoder.encode(sb.toString()));
        return sb.toString();
    }

    @Transactional
    public void delete(Long id, String currentAdmin) {
        User u = require(id);
        if (isSelf(u, currentAdmin)) throw new IllegalArgumentException("You cannot delete your own account.");
        emails.deleteByUserId(id);          // cascades to attachments
        accounts.deleteByUserId(id);
        users.delete(u);
    }

    // ------------------------------------------------------------------ helpers

    private User require(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Account not found."));
    }

    private boolean isSelf(User u, String currentAdmin) {
        return u.getUsername().equals(currentAdmin);
    }

    private void assertUnique(String username, String email, Long selfId) {
        users.findByUsername(username).filter(o -> !o.getId().equals(selfId)).ifPresent(o -> {
            throw new IllegalArgumentException("Username already taken.");
        });
        users.findByEmail(email).filter(o -> !o.getId().equals(selfId)).ifPresent(o -> {
            throw new IllegalArgumentException("Email already registered.");
        });
    }
}
