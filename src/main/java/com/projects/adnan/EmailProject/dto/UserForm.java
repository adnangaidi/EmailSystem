package com.projects.adnan.EmailProject.dto;


import com.projects.adnan.EmailProject.entity.enums.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Admin form: create / edit an account. Password is required on create, optional on edit. */
@Getter
@Setter
public class UserForm {

    private Long id;

    @NotBlank @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Letters, digits, dot, dash and underscore only")
    private String username;

    @NotBlank @Email @Size(max = 150)
    private String email;

    @Size(max = 100)
    private String password;

    @NotNull
    private Role role = Role.USER;

    private boolean enabled = true;
    private boolean locked = false;

    @Valid
    private MailAccountForm mail = new MailAccountForm();
}
