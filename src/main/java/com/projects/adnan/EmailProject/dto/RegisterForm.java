package com.projects.adnan.EmailProject.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterForm {

    @NotBlank @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Letters, digits, dot, dash and underscore only")
    private String username;

    @NotBlank @Email @Size(max = 150)
    private String email;

    @NotBlank @Size(min = 10, max = 100, message = "Password must be 10 to 100 characters")
    private String password;

    @NotBlank
    private String confirmPassword;

    @Valid
    private MailAccountForm mail = new MailAccountForm();
}
