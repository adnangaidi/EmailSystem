package com.projects.adnan.EmailProject.service;

import com.projects.adnan.EmailProject.config.MailProperties;
import com.projects.adnan.EmailProject.dto.MailAccountForm;
import com.projects.adnan.EmailProject.dto.RegisterForm;
import com.projects.adnan.EmailProject.entity.MailAccount;
import com.projects.adnan.EmailProject.entity.User;
import com.projects.adnan.EmailProject.entity.enums.Role;
import com.projects.adnan.EmailProject.exception.ResourceNotFoundException;
import com.projects.adnan.EmailProject.repository.MailAccountRepository;
import com.projects.adnan.EmailProject.repository.UserRepository;
import com.projects.adnan.EmailProject.security.EncryptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final MailAccountRepository accounts;
    private final PasswordEncoder encoder;
    private final EncryptionService crypto;
    private final MailProperties mailProps;

    @Transactional
    public User register(@Valid RegisterForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        checkPassword(form.getPassword());

        String username = form.getUsername().trim();
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsername(username)) throw new IllegalArgumentException("Username already taken.");
        if (users.existsByEmail(email)) throw new IllegalArgumentException("Email already registered.");

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(form.getPassword()));
        user.setRole(Role.USER);                       // self-registration never creates admins
        users.save(user);

        saveMailAccount(user, form.getMail());
        return user;
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = requireByUsername(username);
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        checkPassword(newPassword);
        user.setPasswordHash(encoder.encode(newPassword));
    }

    @Transactional(readOnly = true)
    public User requireByUsername(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    @Transactional(readOnly = true)
    public Long idOf(String username) {
        return requireByUsername(username).getId();
    }

    /** Creates or updates the mailbox settings. */
    @Transactional
    public void saveMailAccount(User user, MailAccountForm f) {
        if (f == null) return;

        Optional<MailAccount> existingOpt = accounts.findByUserId(user.getId());

        boolean hasUsername = f.getMailUsername() != null && !f.getMailUsername().isBlank();
        boolean hasPassword = f.getMailPassword() != null && !f.getMailPassword().isBlank();
        boolean hasSmtpHost = f.getSmtpHost() != null && !f.getSmtpHost().isBlank();
        boolean hasPop3Host = f.getPop3Host() != null && !f.getPop3Host().isBlank();

        // If no existing mailbox and neither password nor hosts are provided, do nothing
        if (existingOpt.isEmpty() && !hasPassword && !hasSmtpHost && !hasPop3Host) {
            return;
        }

        MailAccount acc = existingOpt.orElseGet(() -> {
            MailAccount a = new MailAccount();
            a.setUser(user);
            return a;
        });

        String username = hasUsername ? f.getMailUsername().trim() : user.getEmail();
        acc.setMailUsername(username);

        String defaultSmtpHost = mailProps.getSmtp().getHost() != null ? mailProps.getSmtp().getHost() : "localhost";
        String defaultPop3Host = mailProps.getPop3().getHost() != null ? mailProps.getPop3().getHost() : "localhost";

        acc.setSmtpHost(orDefault(f.getSmtpHost(), defaultSmtpHost));
        acc.setSmtpPort(f.getSmtpPort() != null ? f.getSmtpPort() : mailProps.getSmtp().getPort());
        acc.setPop3Host(orDefault(f.getPop3Host(), defaultPop3Host));
        acc.setPop3Port(f.getPop3Port() != null ? f.getPop3Port() : mailProps.getPop3().getPort());

        if (hasPassword) {
            acc.setMailPasswordEnc(crypto.encryptToBase64(f.getMailPassword()));
        } else if (acc.getMailPasswordEnc() == null) {
            throw new IllegalArgumentException("The mailbox password is required.");
        }
        if (acc.getSmtpHost() == null || acc.getPop3Host() == null) {
            throw new IllegalArgumentException("SMTP and POP3 hosts are required.");
        }

        accounts.save(acc);
    }

    public static void checkPassword(String password) {
        if (password == null || password.length() < 10 || password.length() > 100) {
            throw new IllegalArgumentException("Password must be 10 to 100 characters.");
        }
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
