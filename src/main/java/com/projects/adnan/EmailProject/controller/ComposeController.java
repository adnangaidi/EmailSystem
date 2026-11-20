package com.projects.adnan.EmailProject.controller;

import com.projects.adnan.EmailProject.dto.ComposeForm;
import com.projects.adnan.EmailProject.service.EmailService;
import com.projects.adnan.EmailProject.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/compose")
@Slf4j
@RequiredArgsConstructor
public class ComposeController {

    private final EmailService emailService;
    private final UserService userService;

    @GetMapping
    public String form(@RequestParam(required = false) String to, Model model) {
        ComposeForm form = new ComposeForm();
        form.setTo(to);                              // reply shortcut: /compose?to=a@b.com
        model.addAttribute("form", form);
        return "compose";
    }

    @PostMapping
    public String send(@Valid @ModelAttribute("form") ComposeForm form, BindingResult br,
                       Authentication auth, RedirectAttributes ra) {
        if (br.hasErrors()) return "compose";
        try {
            emailService.send(userService.idOf(auth.getName()), form);
        } catch (IllegalArgumentException | IllegalStateException e) {
            br.reject("send.error", e.getMessage());
            return "compose";
        } catch (MailException e) {
            log.warn("SMTP failure: {}", e.getMessage());
            br.reject("send.error", "The email could not be sent. Check your mailbox settings.");
            return "compose";
        }
        ra.addFlashAttribute("message", "Email sent.");
        return "redirect:/sent";
    }
}
