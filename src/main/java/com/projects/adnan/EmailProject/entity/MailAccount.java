package com.projects.adnan.EmailProject.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mail_accounts")
@Getter
@Setter
@NoArgsConstructor
public class MailAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "smtp_host", nullable = false)
    private String smtpHost;

    @Column(name = "smtp_port", nullable = false)
    private int smtpPort = 587;

    @Column(name = "pop3_host", nullable = false)
    private String pop3Host;

    @Column(name = "pop3_port", nullable = false)
    private int pop3Port = 995;

    @Column(name = "mail_username", nullable = false)
    private String mailUsername;

    /** AES-256-GCM encrypted (Base64: iv + ciphertext) */
    @Column(name = "mail_password_enc", nullable = false, length = 512)
    private String mailPasswordEnc;
}
