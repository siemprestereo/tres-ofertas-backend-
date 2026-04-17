package com.tresofertas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "suspensions")
@Getter
@Setter
public class Suspension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(length = 500)
    private String reason;

    @Column(name = "suspended_at", nullable = false, updatable = false)
    private LocalDateTime suspendedAt = LocalDateTime.now();

    @Column(name = "lifted_at")
    private LocalDateTime liftedAt;

    @Column(nullable = false)
    private Boolean permanent = false;
}
