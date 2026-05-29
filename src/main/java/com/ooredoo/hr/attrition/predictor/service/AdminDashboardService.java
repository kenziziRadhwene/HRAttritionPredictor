package com.ooredoo.hr.attrition.predictor.service;

import com.ooredoo.hr.attrition.predictor.dto.response.AdminDashboardResponse;
import com.ooredoo.hr.attrition.predictor.entity.AuditLog;
import com.ooredoo.hr.attrition.predictor.enums.EAuditAction;
import com.ooredoo.hr.attrition.predictor.enums.ERole;
import com.ooredoo.hr.attrition.predictor.repository.AuditLogRepository;
import com.ooredoo.hr.attrition.predictor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository    userRepository;
    private final AuditLogRepository auditLogRepository;

    // Labels lisibles pour les actions
    private static final Map<EAuditAction, String> ACTION_LABELS = Map.ofEntries(
            Map.entry(EAuditAction.LOGIN,                       "Connexion"),
            Map.entry(EAuditAction.LOGOUT,                      "Déconnexion"),
            Map.entry(EAuditAction.USER_CREATE,                 "Création utilisateur"),
            Map.entry(EAuditAction.USER_UPDATE,                 "Modification utilisateur"),
            Map.entry(EAuditAction.USER_DELETE,                 "Suppression utilisateur"),
            Map.entry(EAuditAction.PROFILE_UPDATE,              "Mise à jour profil"),
            Map.entry(EAuditAction.EMPLOYEE_CREATE,             "Ajout employé"),
            Map.entry(EAuditAction.EMPLOYEE_UPDATE,             "Modification employé"),
            Map.entry(EAuditAction.EMPLOYEE_DELETE,             "Suppression employé"),
            Map.entry(EAuditAction.EMPLOYEE_IMPORT,             "Import employés"),
            Map.entry(EAuditAction.PREDICTION_INDIVIDUELLE,     "Prédiction individuelle"),
            Map.entry(EAuditAction.PREDICTION_BATCH,            "Prédiction batch"),
            Map.entry(EAuditAction.PREDICTION_BATCH_DEPARTEMENT,"Prédiction batch département"),
            Map.entry(EAuditAction.SIMULATION_SALAIRE,          "Simulation salaire"),
            Map.entry(EAuditAction.SIMULATION_POSTE,            "Simulation poste"),
            Map.entry(EAuditAction.SIMULATION_FORMATION,        "Simulation formation")
    );

    // ─────────────────────────────────────
    // Méthode principale
    // ─────────────────────────────────────
    public AdminDashboardResponse getDashboard() {

        // ── KPI 1 : Nombre total d'utilisateurs ──
        long totalUtilisateurs = userRepository.count();

        // ── KPI 2 : Nombre d'actions aujourd'hui ──
        LocalDateTime debutJournee = LocalDate.now().atStartOfDay();
        LocalDateTime finJournee   = debutJournee.plusDays(1);
        List<AuditLog> tousLogs    = auditLogRepository.findAll();

        long actionsAujourdhui = tousLogs.stream()
                .filter(l -> l.getCreatedAt() != null
                        && !l.getCreatedAt().isBefore(debutJournee)
                        && l.getCreatedAt().isBefore(finJournee))
                .count();

        // ── Line Chart : activité des 7 derniers jours ──
        Map<String, Long> activiteHebdomadaire = buildActiviteHebdomadaire(tousLogs);

        // ── Tableau : 5 dernières actions ──
        List<AdminDashboardResponse.DerniereActionDTO> dernieresActions =
                buildDernieresActions(tousLogs);

        // ── Doughnut : répartition par rôle ──
        Map<String, Long> repartitionParRole = buildRepartitionParRole();

        return AdminDashboardResponse.builder()
                .totalUtilisateurs(totalUtilisateurs)
                .actionsAujourdhui(actionsAujourdhui)
                .activiteHebdomadaire(activiteHebdomadaire)
                .dernieresActions(dernieresActions)
                .repartitionParRole(repartitionParRole)
                .build();
    }

    // ─────────────────────────────────────
    // Activité 7 derniers jours (Line Chart)
    // ─────────────────────────────────────
    private Map<String, Long> buildActiviteHebdomadaire(List<AuditLog> logs) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Initialiser les 7 jours avec 0
        Map<String, Long> activite = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            activite.put(LocalDate.now().minusDays(i).format(fmt), 0L);
        }

        LocalDateTime debut7Jours = LocalDate.now().minusDays(6).atStartOfDay();

        logs.stream()
                .filter(l -> l.getCreatedAt() != null
                        && !l.getCreatedAt().isBefore(debut7Jours))
                .forEach(l -> {
                    String jour = l.getCreatedAt().toLocalDate().format(fmt);
                    activite.computeIfPresent(jour, (k, v) -> v + 1);
                });

        return activite;
    }

    // ─────────────────────────────────────
    // 5 dernières actions (Tableau)
    // ─────────────────────────────────────
    private List<AdminDashboardResponse.DerniereActionDTO> buildDernieresActions(List<AuditLog> logs) {
        return logs.stream()
                .filter(l -> l.getCreatedAt() != null)
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .limit(5)
                .map(l -> AdminDashboardResponse.DerniereActionDTO.builder()
                        .id(l.getId())
                        .utilisateur(l.getUser() != null
                                ? l.getUser().getNom() + " " + l.getUser().getPrenom()
                                : "Système")
                        .email(l.getUser() != null ? l.getUser().getEmail() : "-")
                        .role(l.getUser() != null ? l.getUser().getUserRole().name() : "-")
                        .action(l.getAction().name())
                        .actionLabel(ACTION_LABELS.getOrDefault(l.getAction(), l.getAction().name()))
                        .targetTable(l.getTargetTable())
                        .details(l.getDetails())
                        .ipAddress(l.getIpAddress())
                        .createdAt(l.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────
    // Répartition par rôle (Doughnut)
    // ─────────────────────────────────────
    private Map<String, Long> buildRepartitionParRole() {
        Map<String, Long> repartition = new LinkedHashMap<>();
        for (ERole role : ERole.values()) {
            long count = userRepository.findByUserRole(role).size();
            repartition.put(role.name(), count);
        }
        return repartition;
    }
}