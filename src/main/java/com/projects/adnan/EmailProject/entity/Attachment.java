package com.projects.adnan.EmailProject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email_id", nullable = false)
    private EmailMessage email;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type", length = 150)
    private String contentType;

    /** Original (plain) size in bytes */
    private long size;

    /** AES-256-GCM encrypted content */
    @Column(name = "data_enc", columnDefinition = "BLOB", nullable = false)
    private byte[] dataEnc;

    @Column(length = 12, nullable = false)
    private byte[] iv;
}
