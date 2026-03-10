package com.ooredoo.hr.attrition.predictor.dto.response;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data @Builder
public class DashboardStatsResponse {

    // ─── Statistiques globales ───
    private Long totalEmployes;
    private Long totalActifs;
    private Long totalAlertes;
    private Long alertesNonLues;
    private Long totalPredictions;

    // ─── Répartition par risque ───
    private Long risqueEleve;
    private Long risqueMoyen;
    private Long risqueFaible;
    private Double tauxRisqueEleve;

    // ─── Répartition par département ───
    private Map<String, Long> repartitionDepartement;

    // ─── Top 5 employés à risque ───
    private List<EmployeeRisqueDTO> top5Risque;

    // ─── DTO interne ───
    @Data @Builder
    public static class EmployeeRisqueDTO {
        private Long id;
        private String nom;
        private String matricule;
        private String departement;
        private Double probabilite;
        private String niveauRisque;
    }
}