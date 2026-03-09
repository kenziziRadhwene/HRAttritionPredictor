package com.ooredoo.hr.attrition.predictor.entity;

import com.ooredoo.hr.attrition.predictor.enums.EDepartment;
import com.ooredoo.hr.attrition.predictor.enums.EGender;
import com.ooredoo.hr.attrition.predictor.enums.EJobRole;
import com.ooredoo.hr.attrition.predictor.enums.EMaritalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─────────────────────────────────────
    // Informations personnelles
    // ─────────────────────────────────────
    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EGender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EMaritalStatus maritalStatus;

    @Column(nullable = false)
    private Integer distanceFromHome;

    // ─────────────────────────────────────
    // Informations professionnelles
    // ─────────────────────────────────────
    @Column(nullable = false, unique = true)
    private String matricule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EDepartment department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EJobRole jobRole;

    @Column(nullable = false)
    private Integer jobLevel;

    @Column(nullable = false)
    private String businessTravel;

    @Column(nullable = false)
    private String educationField;

    @Column(nullable = false)
    private Integer education;

    // ─────────────────────────────────────
    // Rémunération
    // ─────────────────────────────────────
    @Column(nullable = false)
    private Integer monthlyIncome;

    @Column(nullable = false)
    private Integer dailyRate;

    @Column(nullable = false)
    private Integer hourlyRate;

    @Column(nullable = false)
    private Integer monthlyRate;

    @Column(nullable = false)
    private Integer percentSalaryHike;

    @Column(nullable = false)
    private Integer stockOptionLevel;

    // ─────────────────────────────────────
    // Engagement et satisfaction
    // ─────────────────────────────────────
    @Column(nullable = false)
    private Integer jobSatisfaction;

    @Column(nullable = false)
    private Integer environmentSatisfaction;

    @Column(nullable = false)
    private Integer relationshipSatisfaction;

    @Column(nullable = false)
    private Integer jobInvolvement;

    @Column(nullable = false)
    private Integer workLifeBalance;

    @Column(nullable = false)
    private Integer performanceRating;

    // ─────────────────────────────────────
    // Activité et historique
    // ─────────────────────────────────────
    @Column(nullable = false)
    private Boolean overTime;

    @Column(nullable = false)
    private Integer numCompaniesWorked;

    @Column(nullable = false)
    private Integer totalWorkingYears;

    @Column(nullable = false)
    private Integer yearsAtCompany;

    @Column(nullable = false)
    private Integer yearsInCurrentRole;

    @Column(nullable = false)
    private Integer yearsSinceLastPromotion;

    @Column(nullable = false)
    private Integer yearsWithCurrManager;

    @Column(nullable = false)
    private Integer trainingTimesLastYear;

    // ─────────────────────────────────────
    // Métadonnées
    // ─────────────────────────────────────
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean active = true;

    // ─────────────────────────────────────
    // Relations
    // ─────────────────────────────────────
    @OneToMany(mappedBy = "employee",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    private List<ScoreRisque> scoresRisque;

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