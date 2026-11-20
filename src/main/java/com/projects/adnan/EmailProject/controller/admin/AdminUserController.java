package com.projects.adnan.EmailProject.controller.admin;

import com.projects.adnan.EmailProject.dto.UserForm;
import com.projects.adnan.EmailProject.entity.enums.Role;
import com.projects.adnan.EmailProject.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
//@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService admin;

    public AdminUserController(AdminService admin) {
        this.admin = admin;
    }

    @ModelAttribute("roles")
    public Role[] roles() {
        return Role.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", admin.list(q, page));
        model.addAttribute("q", q);
        model.addAttribute("stats", admin.stats());
        return "admin/users";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new UserForm());
        return "admin/user-form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") UserForm form, BindingResult br, RedirectAttributes ra) {
        if (br.hasErrors()) return "admin/user-form";
        try {
            admin.create(form);
        } catch (IllegalArgumentException e) {
            br.reject("user.error", e.getMessage());
            return "admin/user-form";
        }
        ra.addFlashAttribute("message", "Account created.");
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {

        model.addAttribute("form", admin.toForm(id));
        return "admin/user-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") UserForm form,
                         BindingResult br, Authentication auth, RedirectAttributes ra) {
        form.setId(id);
        if (br.hasErrors()) return "admin/user-form";
        try {
            admin.update(id, form, auth.getName());
        } catch (IllegalArgumentException e) {
            br.reject("user.error", e.getMessage());
            return "admin/user-form";
        }
        ra.addFlashAttribute("message", "Account updated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/lock")
    public String lock(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        return act(ra, () -> admin.setLocked(id, true, auth.getName()), "Account locked.");
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        return act(ra, () -> admin.setLocked(id, false, auth.getName()), "Account unlocked.");
    }

    @PostMapping("/{id}/enable")
    public String enable(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        return act(ra, () -> admin.setEnabled(id, true, auth.getName()), "Account activated.");
    }

    @PostMapping("/{id}/disable")
    public String disable(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        return act(ra, () -> admin.setEnabled(id, false, auth.getName()), "Account deactivated.");
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes ra) {
        String temp = admin.resetPassword(id);
        ra.addFlashAttribute("message", "Temporary password (shown once): " + temp);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        return act(ra, () -> admin.delete(id, auth.getName()), "Account deleted.");
    }

    private String act(RedirectAttributes ra, Runnable action, String success) {
        try {
            action.run();
            ra.addFlashAttribute("message", success);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
