package com.ooredoo.hr.attrition.predictor.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ooredoo.hr.attrition.predictor.enums.ENiveauRisque;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scores_risque")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreRisque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─────────────────────────────────────
    // Résultat ML
    // ─────────────────────────────────────
    @Column(nullable = false)
    private Double probabilite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ENiveauRisque niveauRisque;

    @Column(nullable = false)
    private Double seuilUtilise;

    // ─────────────────────────────────────
    // Facteurs SHAP (stockés en JSON)
    // ─────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String facteursPrincipaux;

    // ─────────────────────────────────────
    // Recommandations (stockées en JSON)
    // ─────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String recommandations;

    // ─────────────────────────────────────
    // Métadonnées
    // ─────────────────────────────────────
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCalcul;

    @Column(nullable = false)
    private String modelVersion;

    // ─────────────────────────────────────
    // Relation avec Employee
    // ─────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonIgnoreProperties({"scoresRisque", "hibernateLazyInitializer"})
    private Employee employee;

    // ─────────────────────────────────────
    // Lifecycle Hook
    // ─────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        dateCalcul = LocalDateTime.now();
    }
}