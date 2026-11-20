package com.projects.adnan.EmailProject.repository;

import com.projects.adnan.EmailProject.entity.MailAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MailAccountRepository extends JpaRepository<MailAccount, Long> {

    Optional<MailAccount> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
    /** Used by the POP3 scheduler to sync every configured account */
    List<MailAccount> findAllByUserEnabledTrueAndUserLockedFalse();

    void deleteByUserId(Long userId);

    @Query("SELECT ma.user.id FROM MailAccount ma WHERE ma.user.enabled = true AND ma.user.locked = false")
    List<Long> findSyncableUserIds();
}
