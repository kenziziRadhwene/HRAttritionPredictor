package com.ooredoo.hr.attrition.predictor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seuils_alerte")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeuilAlerte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─────────────────────────────────────
    // Configuration du seuil
    // ─────────────────────────────────────
    @Column(nullable = false, unique = true)
    private String nom;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Double seuilMinimum;

    @Column(nullable = false)
    private Double seuilMaximum;

    @Column(nullable = false)
    private Boolean actif = true;

    // ─────────────────────────────────────
    // Notification
    // ─────────────────────────────────────
    @Column(nullable = false)
    private Boolean envoyerEmail = true;

    @Column(nullable = false)
    private String emailsDestinataires;

    // ─────────────────────────────────────
    // Métadonnées
    // ─────────────────────────────────────
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String createdBy;

    // ─────────────────────────────────────
    // Lifecycle Hooks
    // ─────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}