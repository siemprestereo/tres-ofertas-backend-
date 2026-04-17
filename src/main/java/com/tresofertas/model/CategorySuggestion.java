package com.tresofertas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "category_suggestions")
@Getter
@Setter
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
}
