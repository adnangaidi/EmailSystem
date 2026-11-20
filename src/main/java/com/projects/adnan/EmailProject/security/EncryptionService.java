package com.projects.adnan.EmailProject.security;


import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM.
 * Key: Base64 of 32 random bytes, provided via env var APP_CRYPTO_KEY.
 * Generate one with:  openssl rand -base64 32
 * application.yml:    app.crypto-key: ${APP_CRYPTO_KEY}
 */
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;       // 96 bits (recommended for GCM)
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final String base64Key;
    private SecretKey key;

    public EncryptionService(@Value("${app.crypto-key}") String base64Key) {
        this.base64Key = base64Key;
    }

    @PostConstruct
    void init() {
        byte[] raw = Base64.getDecoder().decode(base64Key);
        if (raw.length != 32) {
            throw new IllegalStateException("app.crypto-key must be 32 bytes (AES-256), Base64 encoded");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    /** Result holder: store iv next to the ciphertext (e.g. EmailMessage.iv / bodyEnc) */
    public record Encrypted(byte[] iv, byte[] data) {}

    // ---------- bytes (email bodies, attachments) ----------

    public Encrypted encrypt(byte[] plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new Encrypted(iv, cipher.doFinal(plain));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] iv, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Decryption failed (wrong key or tampered data)", e);
        }
    }

    // ---------- strings (email body text) ----------

    public Encrypted encryptString(String plain) {
        return encrypt(plain.getBytes(StandardCharsets.UTF_8));
    }

    public String decryptString(byte[] iv, byte[] data) {
        return new String(decrypt(iv, data), StandardCharsets.UTF_8);
    }

    // ---------- single-column format (MailAccount.mailPasswordEnc) ----------

    /** Base64( iv || ciphertext ) */
    public String encryptToBase64(String plain) {
        Encrypted e = encryptString(plain);
        ByteBuffer buf = ByteBuffer.allocate(e.iv().length + e.data().length);
        buf.put(e.iv()).put(e.data());
        return Base64.getEncoder().encodeToString(buf.array());
    }

    public String decryptFromBase64(String encoded) {
        byte[] all = Base64.getDecoder().decode(encoded);
        byte[] iv = new byte[IV_LENGTH];
        byte[] data = new byte[all.length - IV_LENGTH];
        System.arraycopy(all, 0, iv, 0, IV_LENGTH);
        System.arraycopy(all, IV_LENGTH, data, 0, data.length);
        return decryptString(iv, data);
    }
}
