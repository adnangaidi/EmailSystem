package com.projects.adnan.EmailProject.repository;

import com.projects.adnan.EmailProject.entity.EmailMessage;
import com.projects.adnan.EmailProject.entity.enums.Folder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long> {

    /** Inbox / Sent / Trash listing (pass Sort in the Pageable) */
    Page<EmailMessage> findByUserIdAndFolder(Long userId, Folder folder, Pageable pageable);

    /** Ownership check: a user can only open their own messages */
    Optional<EmailMessage> findByIdAndUserId(Long id, Long userId);

    /** Avoid duplicates when fetching via POP3 */
    boolean existsByUserIdAndMessageIdAndFolder(Long userId, String messageId, Folder folder);

    long countByUserIdAndFolderAndReadFalse(Long userId, Folder folder);

    long countByUserIdAndFolder(Long userId, Folder folder);

    /** Admin dashboard: metadata only, never content */
    long countByFolder(Folder folder);

    @Query("select count(e) from EmailMessage e where e.folder = :folder and "
            + "((e.folder = com.projects.adnan.EmailProject.entity.enums.Folder.SENT and e.sentAt >= :since) "
            + "or (e.folder = com.projects.adnan.EmailProject.entity.enums.Folder.INBOX and e.receivedAt >= :since))")
    long countSince(@Param("folder") Folder folder, @Param("since") LocalDateTime since);

    @Modifying
    @Query("update EmailMessage e set e.read = true where e.id = :id and e.user.id = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    @Query("select coalesce(sum(a.size), 0) from Attachment a where a.email.user.id = :userId")
    long attachmentsSizeByUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from EmailMessage e where e.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    void deleteByUserId(Long id);

    boolean existsByUserIdAndMessageIdAndFolderIn(Long userId, String messageId, List<Folder> inbox);
}
