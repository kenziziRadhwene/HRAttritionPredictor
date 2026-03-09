package com.ooredoo.hr.attrition.predictor.dto.response;

import com.ooredoo.hr.attrition.predictor.enums.ENiveauRisque;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScoreRisqueResponse {

    private Long id;
    private Long employeeId;
    private String employeeNom;
    private String employeeMatricule;
    private Double probabilite;
    private ENiveauRisque niveauRisque;
    private Double seuilUtilise;
    private String facteursPrincipaux;
    private String recommandations;
    private LocalDateTime dateCalcul;
    private String modelVersion;
}