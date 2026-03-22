package com.example.waiter_rating.repository;


import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.UserRole;
import com.example.waiter_rating.model.enums.AuthProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepo extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByEmailAndAuthProvider(String email, AuthProvider authProvider);

    @Query("SELECT u FROM AppUser u WHERE u.suspended = false AND u.email IS NOT NULL")
    List<AppUser> findAllActive();

    @Query("SELECT u FROM AppUser u WHERE u.activeRole = :role AND u.suspended = false AND u.email IS NOT NULL")
    List<AppUser> findActiveByRole(UserRole role);

    @Query("SELECT u FROM AppUser u WHERE u.activeRole = 'PROFESSIONAL' AND u.searchable = true")
    List<AppUser> findSearchableProfessionals();

    @Query("SELECT u FROM AppUser u WHERE u.activeRole = 'PROFESSIONAL' AND u.searchable = true AND u.reputationScore > 0 ORDER BY u.reputationScore DESC")
    List<AppUser> findTopProfessionals(Pageable pageable);

    List<AppUser> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT u FROM AppUser u WHERE u.createdAt >= :since")
    List<AppUser> findByCreatedAtAfter(@Param("since") java.time.LocalDateTime since);

}

