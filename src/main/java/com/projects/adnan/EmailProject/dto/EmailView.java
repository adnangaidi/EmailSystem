package com.projects.adnan.EmailProject.dto;


import com.projects.adnan.EmailProject.entity.enums.Folder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/** Read-only view sent to Thymeleaf. body is null in list views and sanitized HTML in detail views. */
@Value
public class EmailView {
    Long id;
    Folder folder;
    String sender;
    String recipients;
    String subject;
    String body;
    LocalDateTime date;
    boolean read;
    List<AttachmentInfo> attachments;

    @Value
    public static class AttachmentInfo {
        Long id;
        String filename;
        long size;
    }
}
