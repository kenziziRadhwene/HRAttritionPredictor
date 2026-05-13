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

    // ─── Répartition par département (existant) ───
    private Map<String, Long> repartitionDepartement;

    // ─── NOUVEAU : Répartition risque détaillée par département ───
    // Structure : { "DIRECTION_TECHNOLOGIQUE": { "ÉLEVÉ": 45, "MOYEN": 30, "FAIBLE": 10 } }
    private Map<String, Map<String, Long>> repartitionRisqueParDepartement;

    // ─── NOUVEAU : Taux de turnover prédit par département (%) ───
    // Structure : { "DIRECTION_TECHNOLOGIQUE": 52.3, "DIRECTION_SERVICE_CLIENT": 38.1 }
    private Map<String, Double> tauxTurnoverParDepartement;

    // ─── NOUVEAU : Top facteurs de risque globaux ───
    private List<FacteurRisqueGlobalDTO> topFacteursRisque;

    // ─── Top 5 employés à risque ───
    private List<EmployeeRisqueDTO> top5Risque;

    // ─────────────────────────────────────
    // DTOs internes
    // ─────────────────────────────────────

    @Data @Builder
    public static class EmployeeRisqueDTO {
        private Long id;
        private String nom;
        private String matricule;
        private String departement;
        private Double probabilite;
        private String niveauRisque;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FacteurRisqueGlobalDTO {
        private String feature;       // Nom du facteur (ex: JobSatisfaction)
        private String featureLabel;  // Label lisible (ex: Satisfaction au travail)
        private Long   count;         // Nombre d'employés où ce facteur AUGMENTE le risque
        private Double avgShapValue;  // Valeur SHAP moyenne absolue
        private Double pourcentage;   // Pourcentage de contribution relative
    }
}