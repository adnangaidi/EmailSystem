package com.projects.adnan.EmailProject.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Optional mailbox settings. Blank hosts/ports fall back to app.mail.* defaults. */
@Getter
@Setter
public class MailAccountForm {

    private static final String HOST = "^$|^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$";

    @Size(max = 255) @Pattern(regexp = HOST, message = "Invalid host name")
    private String smtpHost;
    @Min(1) @Max(65535)
    private Integer smtpPort;

    @Size(max = 255) @Pattern(regexp = HOST, message = "Invalid host name")
    private String pop3Host;
    @Min(1) @Max(65535)
    private Integer pop3Port;

    /** Mailbox login = the email address used as "From" */
    @Size(max = 255)
    private String mailUsername;
    @Size(max = 200)
    private String mailPassword;
}
