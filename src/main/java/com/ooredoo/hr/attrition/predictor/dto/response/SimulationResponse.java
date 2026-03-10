package com.ooredoo.hr.attrition.predictor.dto.response;

import lombok.*;

@Data @Builder
public class SimulationResponse {

    // Informations employé
    private Long employeeId;
    private String employeeNom;
    private String employeeMatricule;

    // Scénario simulé
    private String typeSimulation;
    private String descriptionScenario;

    // Résultats
    private Double probabiliteActuelle;
    private Double probabiliteSimulee;
    private Double impactPourcentage;
    private String niveauRisqueActuel;
    private String niveauRisqueSimule;

    // Recommandation
    private String recommandation;
    private Boolean actionRecommandee;
}