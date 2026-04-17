package com.tresofertas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "category_suggestions")
public class CategorySuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_by", nullable = false)
    private AppUser suggestedBy;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean reviewed = false;

    private Boolean approved;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public AppUser getSuggestedBy() { return suggestedBy; }
    public void setSuggestedBy(AppUser suggestedBy) { this.suggestedBy = suggestedBy; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getReviewed() { return reviewed; }
    public void setReviewed(Boolean reviewed) { this.reviewed = reviewed; }
    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
