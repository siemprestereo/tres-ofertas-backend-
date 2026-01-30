package com.example.waiter_rating.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "certifications")
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "professional_id", nullable = false)
    private AppUser professional;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "issuer", length = 200)
    private String issuer;

    @Column(name = "date_obtained")
    private LocalDate dateObtained;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getProfessional() {
        return professional;
    }

    public void setProfessional(AppUser professional) {
        this.professional = professional;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public LocalDate getDateObtained() {
        return dateObtained;
    }

    public void setDateObtained(LocalDate dateObtained) {
        this.dateObtained = dateObtained;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
}
