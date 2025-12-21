package com.example.waiter_rating.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "businesses")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 200)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false)
    private BusinessType businessType;

    @Column(length = 50)
    private String phone; // Agregamos teléfono que puede ser útil
}