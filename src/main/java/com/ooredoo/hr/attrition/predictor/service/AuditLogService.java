package com.ooredoo.hr.attrition.predictor.service;

import com.ooredoo.hr.attrition.predictor.dto.response.AuditLogResponse;
import com.ooredoo.hr.attrition.predictor.entity.AuditLog;
import com.ooredoo.hr.attrition.predictor.entity.User;
import com.ooredoo.hr.attrition.predictor.enums.EAuditAction;
import com.ooredoo.hr.attrition.predictor.repository.AuditLogRepository;
import com.ooredoo.hr.attrition.predictor.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final HttpServletRequest httpRequest;

    private static final Map<EAuditAction, String> ACTION_LABELS = Map.ofEntries(
            Map.entry(EAuditAction.LOGIN,                        "Connexion"),
            Map.entry(EAuditAction.LOGOUT,                       "Déconnexion"),
            Map.entry(EAuditAction.USER_CREATE,                  "Création utilisateur"),
            Map.entry(EAuditAction.USER_UPDATE,                  "Modification utilisateur"),
            Map.entry(EAuditAction.USER_DELETE,                  "Suppression utilisateur"),
            Map.entry(EAuditAction.PROFILE_UPDATE,               "Mise à jour profil"),
            Map.entry(EAuditAction.EMPLOYEE_CREATE,              "Ajout employé"),
            Map.entry(EAuditAction.EMPLOYEE_UPDATE,              "Modification employé"),
            Map.entry(EAuditAction.EMPLOYEE_DELETE,              "Suppression employé"),
            Map.entry(EAuditAction.EMPLOYEE_IMPORT,              "Import employés"),
            Map.entry(EAuditAction.PREDICTION_INDIVIDUELLE,      "Prédiction individuelle"),
            Map.entry(EAuditAction.PREDICTION_BATCH,             "Prédiction batch"),
            Map.entry(EAuditAction.PREDICTION_BATCH_DEPARTEMENT, "Prédiction batch département"),
            Map.entry(EAuditAction.SIMULATION_SALAIRE,           "Simulation salaire"),
            Map.entry(EAuditAction.SIMULATION_POSTE,             "Simulation poste"),
            Map.entry(EAuditAction.SIMULATION_FORMATION,         "Simulation formation")
    );

    // ─── Log avec user depuis le SecurityContext ───────────────
    public void log(EAuditAction action,
                    String targetTable,
                    Long targetId,
                    String details) {
        logInternal(action, targetTable, targetId, details, getCurrentUser());
    }

    // ─── Log avec user explicite (pour LOGIN) ─────────────────
    public void log(EAuditAction action,
                    String targetTable,
                    Long targetId,
                    String details,
                    User user) {
        logInternal(action, targetTable, targetId, details, user);
    }

    // ─── Méthode interne commune ──────────────────────────────
    private void logInternal(EAuditAction action,
                             String targetTable,
                             Long targetId,
                             String details,
                             User user) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .targetTable(targetTable)
                    .targetId(targetId)
                    .details(details)
                    .ipAddress(getClientIp())
                    .build();

            auditLogRepository.save(auditLog);
            log.info("📋 AuditLog: {} | table={} | id={} | user={}",
                    action, targetTable, targetId,
                    user != null ? user.getEmail() : "anonyme");

        } catch (Exception e) {
            log.error("❌ Erreur AuditLog: {}", e.getMessage());
        }
    }

    // ─── Liste complète sans filtre (utilisée ailleurs) ───────
    public List<AuditLogResponse> getAll() {
        return auditLogRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Liste filtrée + paginée ──────────────────────────────
    public Page<AuditLogResponse> getFiltered(String search,
                                              String role,
                                              String action,
                                              int page,
                                              int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository
                .findFiltered(
                        search == null ? "" : search.trim(),
                        role   == null ? "" : role.trim(),
                        action == null ? "" : action.trim(),
                        pageable
                )
                .map(this::toResponse);
    }

    // ─── Mapper entité → DTO ──────────────────────────────────
    private AuditLogResponse toResponse(AuditLog l) {
        return AuditLogResponse.builder()
                .id(l.getId())
                .utilisateur(l.getUser() != null
                        ? l.getUser().getNom() + " " + l.getUser().getPrenom()
                        : "Système")
                .email(l.getUser() != null ? l.getUser().getEmail() : "-")
                .role(l.getUser() != null ? l.getUser().getUserRole().name() : "-")
                .action(l.getAction().name())
                .actionLabel(ACTION_LABELS.getOrDefault(l.getAction(), l.getAction().name()))
                .targetTable(l.getTargetTable())
                .targetId(l.getTargetId())
                .details(l.getDetails())
                .ipAddress(l.getIpAddress())
                .createdAt(l.getCreatedAt())
                .build();
    }

    // ─── Helpers ──────────────────────────────────────────────
    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || auth.getPrincipal().equals("anonymousUser")) {
                return null;
            }
            return userRepository.findByEmail(auth.getName()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp() {
        try {
            String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return httpRequest.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}