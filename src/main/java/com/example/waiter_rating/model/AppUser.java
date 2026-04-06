package com.example.waiter_rating.model;

import com.example.waiter_rating.model.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========== CAMPOS COMUNES ==========
    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String location;

    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;

    @Column(length = 255)
    private String password;

    @Column(length = 255)
    private String profilePicture;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(length = 20)
    private String provider; // "GOOGLE" o "LOCAL"

    @Column(length = 100)
    private String providerId;

    @Column(name = "terms_accepted", nullable = false)
    @Builder.Default
    private Boolean termsAccepted = false;

    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    // ========== CONTROL DE ROL ==========
    @Enumerated(EnumType.STRING)
    @Column(name = "active_role", nullable = false, length = 20)
    @Builder.Default
    private UserRole activeRole = UserRole.CLIENT;

    @Column(name = "last_role_switch_at")
    private LocalDateTime lastRoleSwitchAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean suspended = false;

    // ========== CAMPOS DE PROFESSIONAL (nullable) ==========
    @Column(name = "profession_type", length = 50)
    private String professionType; // legacy — se mantiene por compatibilidad

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "professional_profession_types", joinColumns = @JoinColumn(name = "professional_id"))
    @Column(name = "profession_type")
    @Builder.Default
    private Set<String> professionTypes = new HashSet<>();

    @Column(length = 100)
    private String professionalTitle;

    @Column(name = "reputation_score")
    @Builder.Default
    private Double reputationScore = 0.0;

    @Column(name = "total_ratings")
    @Builder.Default
    private Integer totalRatings = 0;

    @Column(name = "monthly_workplace_changes")
    @Builder.Default
    private Integer monthlyWorkplaceChanges = 0;

    @Column(name = "last_workplace_change_date")
    private LocalDate lastWorkplaceChangeDate;

    @Column(name = "searchable")
    @Builder.Default
    private Boolean searchable = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;




    // ========== RELACIONES ==========
    // CV (solo para professionals)
    @OneToOne(mappedBy = "professional", cascade = CascadeType.ALL, orphanRemoval = true)
    private Cv cv;

    // Ratings recibidos (solo para professionals)
    @OneToMany(mappedBy = "professional", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Rating> ratingsReceived = new ArrayList<>();

    // Ratings dados (solo para clients)
    @OneToMany(mappedBy = "client", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<Rating> ratingsGiven = new ArrayList<>();

    // Work history (solo para professionals)
    @OneToMany(mappedBy = "professional", cascade = CascadeType.ALL)
    @Builder.Default
    private List<WorkHistory> workHistory = new ArrayList<>();

    // Educación (solo para professionals)
    @OneToMany(mappedBy = "professional", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Education> education = new ArrayList<>();

    // Profesionales favoritos (solo para clients)
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    @Builder.Default
    private List<FavoriteProfessional> favoriteProfessionals = new ArrayList<>();

    // ========== TIMESTAMPS ==========
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ========== MÉTODOS HELPER ==========

    public boolean isProfessional() {
        return activeRole == UserRole.PROFESSIONAL;
    }

    public boolean isClient() {
        return activeRole == UserRole.CLIENT;
    }

    public String getUserType() {
        return activeRole.name();
    }

    // Métodos de reputación (solo para professionals)
    public Double getAverageRating() {
        if (ratingsReceived == null || ratingsReceived.isEmpty()) {
            return 0.0;
        }
        return ratingsReceived.stream()
                .mapToDouble(Rating::getScore)
                .average()
                .orElse(0.0);
    }

    public Integer getTotalRatingsCount() {
        return ratingsReceived != null ? ratingsReceived.size() : 0;
    }

    public List<Business> getCurrentWorkplaces() {
        if (workHistory == null) {
            return new ArrayList<>();
        }
        return workHistory.stream()
                .filter(WorkHistory::getIsActive)
                .map(WorkHistory::getBusiness)
                .toList();
    }

    public boolean canChangeWorkplace() {
        if (lastWorkplaceChangeDate == null) {
            return true;
        }

        LocalDate now = LocalDate.now();
        if (lastWorkplaceChangeDate.getMonth() != now.getMonth() ||
                lastWorkplaceChangeDate.getYear() != now.getYear()) {
            return true;
        }

        return monthlyWorkplaceChanges < 3;
    }

    public void registerWorkplaceChange() {
        LocalDate now = LocalDate.now();

        if (lastWorkplaceChangeDate == null ||
                lastWorkplaceChangeDate.getMonth() != now.getMonth() ||
                lastWorkplaceChangeDate.getYear() != now.getYear()) {
            monthlyWorkplaceChanges = 1;
        } else {
            monthlyWorkplaceChanges++;
        }

        lastWorkplaceChangeDate = now;
    }

    // Restricción de cambio de rol
    public boolean canSwitchRole() {
        if (lastRoleSwitchAt == null) {
            return true;
        }

        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        return lastRoleSwitchAt.isBefore(sixMonthsAgo);
    }

    public LocalDateTime getNextAllowedRoleSwitchDate() {
        if (lastRoleSwitchAt == null) {
            return LocalDateTime.now();
        }
        return lastRoleSwitchAt.plusMonths(6);
    }

}