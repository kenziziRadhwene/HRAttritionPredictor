package com.ooredoo.hr.attrition.predictor.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolutionMensuelleDTO {
    private String mois;        // Format "2026-01"
    private String moisLabel;   // Format "Jan 2026"
    private Long risqueEleve;
    private Long risqueMoyen;
    private Double tauxRisqueGlobal;
}