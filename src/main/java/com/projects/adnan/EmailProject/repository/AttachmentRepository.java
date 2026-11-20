package com.projects.adnan.EmailProject.repository;

import com.projects.adnan.EmailProject.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByEmailId(Long emailId);

    /** Ownership check: download only attachments of the user's own emails */
    Optional<Attachment> findByIdAndEmailUserId(Long id, Long userId);
}
