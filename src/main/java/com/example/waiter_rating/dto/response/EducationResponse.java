package com.example.waiter_rating.dto.response;


import java.time.LocalDate;

public class EducationResponse {
    private Long id;
    private String institution;
    private String degree;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean currentlyStudying;
    private String description;

    // Constructor vacío
    public EducationResponse() {}

    // Constructor completo
    public EducationResponse(Long id, String institution, String degree, LocalDate startDate,
                        LocalDate endDate, Boolean currentlyStudying, String description) {
        this.id = id;
        this.institution = institution;
        this.degree = degree;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentlyStudying = currentlyStudying;
        this.description = description;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Boolean getCurrentlyStudying() { return currentlyStudying; }
    public void setCurrentlyStudying(Boolean currentlyStudying) { this.currentlyStudying = currentlyStudying; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
