package com.projects.adnan.EmailProject.controller;

import com.projects.adnan.EmailProject.dto.EmailView;
import com.projects.adnan.EmailProject.entity.enums.Folder;
import com.projects.adnan.EmailProject.service.EmailService;
import com.projects.adnan.EmailProject.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
@Slf4j
@RequiredArgsConstructor
public class InboxController {

    private final EmailService emailService;
    private final UserService userService;

    @GetMapping("/inbox")
    public String inbox(Authentication auth, @RequestParam(defaultValue = "0") int page, Model model) {
        return list(auth, Folder.INBOX, page, model);
    }

    @GetMapping("/sent")
    public String sent(Authentication auth, @RequestParam(defaultValue = "0") int page, Model model) {
        return list(auth, Folder.SENT, page, model);
    }

    /** Reuses the inbox.html template (model attribute "folder" tells which one it is). */
    @GetMapping("/trash")
    public String trash(Authentication auth, @RequestParam(defaultValue = "0") int page, Model model) {
        return list(auth, Folder.TRASH, page, model);
    }

    @PostMapping("/inbox/sync")
    public String sync(Authentication auth, RedirectAttributes ra) {
        try {
            int added = emailService.syncInbox(userId(auth));
            ra.addFlashAttribute("message", added + " new message(s).");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.warn("Manual sync failed", e);
            ra.addFlashAttribute("error", "Could not fetch emails. Check your mailbox settings.");
        }
        return "redirect:/inbox";
    }

    @GetMapping("/message/{id}")
    public String view(@PathVariable Long id, Authentication auth, Model model) {
        EmailView email = emailService.get(userId(auth), id);
        model.addAttribute("email", email);
        return "message-view";
    }

    @PostMapping("/message/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        emailService.delete(userId(auth), id);
        ra.addFlashAttribute("message", "Message deleted.");
        return "redirect:/inbox";
    }

    @GetMapping("/attachment/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id, Authentication auth) {
        EmailService.AttachmentContent file = emailService.getAttachment(userId(auth), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)    // never render user content inline
                .body(file.data());
    }

    private String list(Authentication auth, Folder folder, int page, Model model) {
        Long uid = userId(auth);
        model.addAttribute("page", emailService.list(uid, folder, Math.max(page, 0)));
        model.addAttribute("folder", folder.name());
        model.addAttribute("unread", emailService.unreadCount(uid));
        return folder == Folder.SENT ? "sent" : "inbox";
    }

    private Long userId(Authentication auth) {
        return userService.idOf(auth.getName());
    }
}
