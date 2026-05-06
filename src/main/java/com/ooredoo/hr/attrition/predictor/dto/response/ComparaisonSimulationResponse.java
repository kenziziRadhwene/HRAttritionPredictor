package com.ooredoo.hr.attrition.predictor.dto.response;

import lombok.*;
import java.util.List;

@Data @Builder
public class ComparaisonSimulationResponse {

    private Long employeeId;
    private String employeeNom;
    private String employeeMatricule;
    private Double probabiliteActuelle;
    private String niveauRisqueActuel;

    // Les 5 simulations
    private SimulationResponse simulationSalaire10;
    private SimulationResponse simulationSalaire20;
    private SimulationResponse simulationSalaire30;
    private SimulationResponse simulationPoste;
    private SimulationResponse simulationFormation;

    // Meilleure recommandation
    private String meilleureAction;
    private String typeSimulationRecommande;
    private Double meilleurImpact;
}