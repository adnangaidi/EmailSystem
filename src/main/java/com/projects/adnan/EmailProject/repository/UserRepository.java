package com.projects.adnan.EmailProject.repository;


import com.projects.adnan.EmailProject.entity.User;
import com.projects.adnan.EmailProject.entity.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    long countByLockedTrue();

    long countByEnabledFalse();

    /** Admin module: search by username or email */
    @Query("""
           select u from User u
           where lower(u.username) like lower(concat('%', :q, '%'))
              or lower(u.email) like lower(concat('%', :q, '%'))
           """)
    Page<User> search(@Param("q") String q, Pageable pageable);
}
