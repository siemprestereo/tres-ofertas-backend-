package com.example.waiter_rating.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_users_email", columnNames = "email")
})
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(length = 20)
    private String phone;  // Campo agregado en V17

    @Column(length = 100)
    private String location;  // ← NUEVO: Ciudad, País (ej: "Buenos Aires, Argentina")

    @Column(length = 100)
    private String professionalTitle;  // ← NUEVO: Título profesional (ej: "Mesero Senior")

    @Column(length = 255)
    private String password;

    @Column(length = 255)
    private String profilePicture;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Column(length = 20)
    private String provider; // "GOOGLE" o "LOCAL"

    @Column(length = 100)
    private String providerId; // ID del usuario en Google OAuth

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Método abstracto para obtener el tipo de usuario
    public abstract String getUserType();
}