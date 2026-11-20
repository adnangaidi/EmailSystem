package com.projects.adnan.EmailProject.controller;

import com.projects.adnan.EmailProject.dto.RegisterForm;
import com.projects.adnan.EmailProject.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/")
    public String root() {
        return "redirect:/inbox";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult br) {
        if (br.hasErrors()) return "register";
        try {
            userService.register(form);
        } catch (IllegalArgumentException e) {
            br.reject("register.error", e.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }
}
