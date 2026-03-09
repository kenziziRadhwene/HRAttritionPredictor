package com.ooredoo.hr.attrition.predictor.dto.request;

import com.ooredoo.hr.attrition.predictor.enums.EDepartment;
import com.ooredoo.hr.attrition.predictor.enums.EGender;
import com.ooredoo.hr.attrition.predictor.enums.EJobRole;
import com.ooredoo.hr.attrition.predictor.enums.EMaritalStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EmployeeRequest {

    // ─────────────────────────────────────
    // Informations personnelles
    // ─────────────────────────────────────
    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    private String email;

    @NotNull(message = "L'âge est obligatoire")
    @Min(value = 18, message = "L'âge minimum est 18 ans")
    @Max(value = 65, message = "L'âge maximum est 65 ans")
    private Integer age;

    @NotNull(message = "Le genre est obligatoire")
    private EGender gender;

    @NotNull(message = "Le statut marital est obligatoire")
    private EMaritalStatus maritalStatus;

    @NotNull(message = "La distance domicile-travail est obligatoire")
    @Min(value = 0, message = "La distance ne peut pas être négative")
    private Integer distanceFromHome;

    // ─────────────────────────────────────
    // Informations professionnelles
    // ─────────────────────────────────────
    @NotBlank(message = "Le matricule est obligatoire")
    private String matricule;

    @NotNull(message = "Le département est obligatoire")
    private EDepartment department;

    @NotNull(message = "Le rôle est obligatoire")
    private EJobRole jobRole;

    @NotNull(message = "Le niveau de poste est obligatoire")
    @Min(value = 1, message = "Le niveau minimum est 1")
    @Max(value = 5, message = "Le niveau maximum est 5")
    private Integer jobLevel;

    @NotBlank(message = "Les déplacements professionnels sont obligatoires")
    private String businessTravel;

    @NotBlank(message = "Le domaine d'études est obligatoire")
    private String educationField;

    @NotNull(message = "Le niveau d'éducation est obligatoire")
    @Min(value = 1) @Max(value = 5)
    private Integer education;

    // ─────────────────────────────────────
    // Rémunération
    // ─────────────────────────────────────
    @NotNull(message = "Le salaire mensuel est obligatoire")
    @Min(value = 0)
    private Integer monthlyIncome;

    @NotNull @Min(value = 0)
    private Integer dailyRate;

    @NotNull @Min(value = 0)
    private Integer hourlyRate;

    @NotNull @Min(value = 0)
    private Integer monthlyRate;

    @NotNull @Min(value = 0) @Max(value = 100)
    private Integer percentSalaryHike;

    @NotNull @Min(value = 0) @Max(value = 3)
    private Integer stockOptionLevel;

    // ─────────────────────────────────────
    // Engagement et satisfaction
    // ─────────────────────────────────────
    @NotNull @Min(value = 1) @Max(value = 4)
    private Integer jobSatisfaction;

    @NotNull @Min(value = 1) @Max(value = 4)
    private Integer environmentSatisfaction;

    @NotNull @Min(value = 1) @Max(value = 4)
    private Integer relationshipSatisfaction;

    @NotNull @Min(value = 1) @Max(value = 4)
    private Integer jobInvolvement;

    @NotNull @Min(value = 1) @Max(value = 4)
    private Integer workLifeBalance;

    @NotNull @Min(value = 1) @Max(value = 4)
    private Integer performanceRating;

    // ─────────────────────────────────────
    // Activité et historique
    // ─────────────────────────────────────
    @NotNull
    private Boolean overTime;

    @NotNull @Min(value = 0)
    private Integer numCompaniesWorked;

    @NotNull @Min(value = 0)
    private Integer totalWorkingYears;

    @NotNull @Min(value = 0)
    private Integer yearsAtCompany;

    @NotNull @Min(value = 0)
    private Integer yearsInCurrentRole;

    @NotNull @Min(value = 0)
    private Integer yearsSinceLastPromotion;

    @NotNull @Min(value = 0)
    private Integer yearsWithCurrManager;

    @NotNull @Min(value = 0)
    private Integer trainingTimesLastYear;
}