package com.ooredoo.hr.attrition.predictor.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    // ─── KPIs ───
    private Long totalUtilisateurs;
    private Long actionsAujourdhui;

    // ─── Activité des utilisateurs (Line Chart) ───
    // Structure : { "2025-05-23": 12, "2025-05-24": 8, ... } — 7 derniers jours
    private Map<String, Long> activiteHebdomadaire;

    // ─── 5 dernières actions (Tableau) ───
    private List<DerniereActionDTO> dernieresActions;

    // ─── Répartition des utilisateurs (Doughnut) ───
    // Structure : { "ADMIN": 2, "RESPONSABLE_RH": 5, "MANAGER": 8 }
    private Map<String, Long> repartitionParRole;

    // ─────────────────────────────────────
    // DTO interne — ligne du tableau
    // ─────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DerniereActionDTO {
        private Long      id;
        private String    utilisateur;    // "Nom Prénom"
        private String    email;
        private String    role;
        private String    action;         // EAuditAction.name()
        private String    actionLabel;    // libellé lisible
        private String    targetTable;
        private String    details;
        private String    ipAddress;
        private LocalDateTime createdAt;
    }
}