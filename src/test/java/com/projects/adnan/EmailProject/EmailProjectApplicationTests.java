package com.projects.adnan.EmailProject;

import com.projects.adnan.EmailProject.dto.MailAccountForm;
import com.projects.adnan.EmailProject.dto.RegisterForm;
import com.projects.adnan.EmailProject.dto.UserForm;
import com.projects.adnan.EmailProject.entity.MailAccount;
import com.projects.adnan.EmailProject.entity.User;
import com.projects.adnan.EmailProject.entity.enums.Role;
import com.projects.adnan.EmailProject.repository.MailAccountRepository;
import com.projects.adnan.EmailProject.repository.UserRepository;
import com.projects.adnan.EmailProject.security.EncryptionService;
import com.projects.adnan.EmailProject.service.AdminService;
import com.projects.adnan.EmailProject.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmailProjectApplicationTests {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailAccountRepository mailAccountRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Test
    void testCreateAndUpdateUserWithMail() {
        String username = "testuser_" + System.currentTimeMillis();
        String email = username + "@example.com";

        UserForm form = new UserForm();
        form.setUsername(username);
        form.setEmail(email);
        form.setPassword("password12345");
        form.setRole(Role.USER);
        form.setEnabled(true);

        MailAccountForm mail = new MailAccountForm();
        mail.setMailUsername(email);
        mail.setMailPassword("mailpass123");
        mail.setSmtpHost("smtp.example.com");
        mail.setSmtpPort(587);
        mail.setPop3Host("pop.example.com");
        mail.setPop3Port(995);
        form.setMail(mail);

        User created = adminService.create(form);
        assertNotNull(created.getId());

        Optional<MailAccount> accountOpt = mailAccountRepository.findByUserId(created.getId());
        assertTrue(accountOpt.isPresent(), "MailAccount should exist after create");
        MailAccount account = accountOpt.get();
        assertEquals("smtp.example.com", account.getSmtpHost());
        assertEquals(587, account.getSmtpPort());
        assertEquals("pop.example.com", account.getPop3Host());
        assertEquals(995, account.getPop3Port());
        assertEquals(email, account.getMailUsername());
        assertNotNull(account.getId(), "MailAccount ID must not be null!");
        assertEquals("mailpass123", encryptionService.decryptFromBase64(account.getMailPasswordEnc()));

        // Test toForm
        UserForm editForm = adminService.toForm(created.getId());
        assertEquals("smtp.example.com", editForm.getMail().getSmtpHost());
        assertEquals(587, editForm.getMail().getSmtpPort());
        assertEquals("pop.example.com", editForm.getMail().getPop3Host());
        assertEquals(995, editForm.getMail().getPop3Port());

        // Test update without changing password
        editForm.getMail().setSmtpHost("smtp.updated.com");
        editForm.getMail().setPop3Host("pop.updated.com");
        editForm.getMail().setMailPassword(null); // keep existing password
        adminService.update(created.getId(), editForm, "admin");

        MailAccount updated = mailAccountRepository.findByUserId(created.getId()).orElseThrow();
        assertEquals("smtp.updated.com", updated.getSmtpHost());
        assertEquals("pop.updated.com", updated.getPop3Host());
        assertEquals("mailpass123", encryptionService.decryptFromBase64(updated.getMailPasswordEnc()));
    }

    @Test
    void testExistingUserAddAndEditMailbox() {
        // User 'admin' exists from data.sql, initially without mail_accounts record
        User admin = userRepository.findByUsername("admin").orElseThrow();

        UserForm editForm = adminService.toForm(admin.getId());
        // Configure SMTP and POP3 for admin
        editForm.getMail().setSmtpHost("smtp.adminserver.com");
        editForm.getMail().setSmtpPort(465);
        editForm.getMail().setPop3Host("pop.adminserver.com");
        editForm.getMail().setPop3Port(995);
        editForm.getMail().setMailPassword("adminMailPass1!");

        adminService.update(admin.getId(), editForm, "admin");

        MailAccount savedAccount = mailAccountRepository.findByUserId(admin.getId()).orElseThrow();
        assertEquals("smtp.adminserver.com", savedAccount.getSmtpHost());
        assertEquals(465, savedAccount.getSmtpPort());
        assertEquals("pop.adminserver.com", savedAccount.getPop3Host());
        assertEquals("admin@example.com", savedAccount.getMailUsername());
        assertEquals("adminMailPass1!", encryptionService.decryptFromBase64(savedAccount.getMailPasswordEnc()));

        // Now edit again, leaving password blank
        UserForm editForm2 = adminService.toForm(admin.getId());
        assertEquals("smtp.adminserver.com", editForm2.getMail().getSmtpHost());
        editForm2.getMail().setSmtpHost("smtp.admin2.com");
        editForm2.getMail().setMailPassword(""); // blank -> should keep existing

        adminService.update(admin.getId(), editForm2, "admin");

        MailAccount updatedAccount = mailAccountRepository.findByUserId(admin.getId()).orElseThrow();
        assertEquals("smtp.admin2.com", updatedAccount.getSmtpHost());
        assertEquals("adminMailPass1!", encryptionService.decryptFromBase64(updatedAccount.getMailPasswordEnc()));
    }

    @Test
    void testRegisterWithAndWithoutMail() {
        // 1. Register with mail
        String u1 = "reg_mail_" + System.currentTimeMillis();
        RegisterForm f1 = new RegisterForm();
        f1.setUsername(u1);
        f1.setEmail(u1 + "@example.com");
        f1.setPassword("longPassword123!");
        f1.setConfirmPassword("longPassword123!");

        MailAccountForm m1 = new MailAccountForm();
        m1.setMailPassword("secretPass1");
        m1.setSmtpHost("smtp.reg.com");
        m1.setPop3Host("pop.reg.com");
        f1.setMail(m1);

        User registered1 = userService.register(f1);
        Optional<MailAccount> acc1 = mailAccountRepository.findByUserId(registered1.getId());
        assertTrue(acc1.isPresent(), "Registered user with mail details should have MailAccount");
        assertEquals("smtp.reg.com", acc1.get().getSmtpHost());
        assertEquals(u1 + "@example.com", acc1.get().getMailUsername());

        // 2. Register without mail
        String u2 = "reg_nomail_" + System.currentTimeMillis();
        RegisterForm f2 = new RegisterForm();
        f2.setUsername(u2);
        f2.setEmail(u2 + "@example.com");
        f2.setPassword("longPassword123!");
        f2.setConfirmPassword("longPassword123!");
        f2.setMail(new MailAccountForm()); // empty

        User registered2 = userService.register(f2);
        Optional<MailAccount> acc2 = mailAccountRepository.findByUserId(registered2.getId());
        assertTrue(acc2.isEmpty(), "Registered user without mail details should NOT have MailAccount");
    }
}
