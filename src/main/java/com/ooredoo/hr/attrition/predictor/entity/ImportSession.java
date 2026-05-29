package com.ooredoo.hr.attrition.predictor.entity;

import com.ooredoo.hr.attrition.predictor.enums.EStatutImport;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(nullable = false)
    private String fichierNom;

    @Column(nullable = false)
    private Long fichierTaille;

    @Column(nullable = false)
    private Integer totalLignes;

    @Column(nullable = false)
    private Integer nbCrees;

    @Column(nullable = false)
    private Integer nbMisAJour;

    @Column(nullable = false)
    private Integer nbIgnores;

    @Column(nullable = false)
    private Integer nbErreurs;

    @Column(nullable = false)
    private Boolean annule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EStatutImport statut;

    @Column(columnDefinition = "TEXT")
    private String detailsErreurs;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}