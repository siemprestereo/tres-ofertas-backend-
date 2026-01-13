package com.example.waiter_rating.dto.response;


import java.time.LocalDate;

public class CertificationResponse {
    private Long id;
    private String name;
    private String issuer;
    private LocalDate dateObtained;
    private LocalDate expiryDate;

    // Constructor vacío
    public CertificationResponse() {}

    // Constructor completo
    public CertificationResponse(Long id, String name, String issuer,
                            LocalDate dateObtained, LocalDate expiryDate) {
        this.id = id;
        this.name = name;
        this.issuer = issuer;
        this.dateObtained = dateObtained;
        this.expiryDate = expiryDate;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public LocalDate getDateObtained() { return dateObtained; }
    public void setDateObtained(LocalDate dateObtained) { this.dateObtained = dateObtained; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
}
