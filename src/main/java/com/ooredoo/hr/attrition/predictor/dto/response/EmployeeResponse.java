package com.ooredoo.hr.attrition.predictor.dto.response;

import com.ooredoo.hr.attrition.predictor.enums.EDepartment;
import com.ooredoo.hr.attrition.predictor.enums.EGender;
import com.ooredoo.hr.attrition.predictor.enums.EJobRole;
import com.ooredoo.hr.attrition.predictor.enums.EMaritalStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmployeeResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Integer age;
    private EGender gender;
    private EMaritalStatus maritalStatus;
    private Integer distanceFromHome;
    private String matricule;
    private EDepartment department;
    private EJobRole jobRole;
    private Integer jobLevel;
    private String businessTravel;
    private Integer monthlyIncome;
    private Integer stockOptionLevel;
    private Integer jobSatisfaction;
    private Integer environmentSatisfaction;
    private Integer workLifeBalance;
    private Boolean overTime;
    private Integer yearsAtCompany;
    private Integer yearsSinceLastPromotion;
    private Boolean active;
    private LocalDateTime createdAt;

    // Dernier score de risque (optionnel)
    private Double derniereProbabilite;
    private String dernierNiveauRisque;
    private LocalDateTime derniereDateCalcul;

    // Informations personnelles manquantes
    private String educationField;
    private Integer education;

    // Informations professionnelles manquantes
    private Integer yearsInCurrentRole;
    private Integer yearsWithCurrManager;
    private Integer trainingTimesLastYear;
    private Integer numCompaniesWorked;
    private Integer totalWorkingYears;

    // Rémunération manquante
    private Integer dailyRate;
    private Integer hourlyRate;
    private Integer monthlyRate;
    private Integer percentSalaryHike;

    // Satisfaction manquante
    private Integer relationshipSatisfaction;
    private Integer jobInvolvement;
    private Integer performanceRating;
}