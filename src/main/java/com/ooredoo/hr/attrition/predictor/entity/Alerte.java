package com.ooredoo.hr.attrition.predictor.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ooredoo.hr.attrition.predictor.enums.EStatutAlerte;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alertes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alerte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─────────────────────────────────────
    // Contenu de l'alerte
    // ─────────────────────────────────────
    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private Double probabilite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EStatutAlerte statut;

    // ─────────────────────────────────────
    // Destinataires
    // ─────────────────────────────────────
    @Column(nullable = false)
    private String emailDestinataire;

    @Column(nullable = false)
    private Boolean emailEnvoye = false;

    @Column
    private LocalDateTime dateEnvoi;

    // ─────────────────────────────────────
    // Métadonnées
    // ─────────────────────────────────────
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private LocalDateTime dateModification;

    // ─────────────────────────────────────
    // Relations
    // ─────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonIgnoreProperties({"scoresRisque", "alertes", "hibernateLazyInitializer"})
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "score_risque_id", nullable = false)
    @JsonIgnoreProperties({"employee", "hibernateLazyInitializer"})
    private ScoreRisque scoreRisque;

    // ─────────────────────────────────────
    // Lifecycle Hooks
    // ─────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        dateCreation      = LocalDateTime.now();
        dateModification  = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }
}