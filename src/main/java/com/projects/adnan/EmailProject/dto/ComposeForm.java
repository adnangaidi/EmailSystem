package com.projects.adnan.EmailProject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ComposeForm {

    @NotBlank @Size(max = 2000)
    private String to;

    @Size(max = 2000)
    private String cc;

    @NotBlank @Size(max = 200)
    private String subject;

    @NotBlank @Size(max = 100_000)
    private String body;

    private List<MultipartFile> attachments = new ArrayList<>();
}
