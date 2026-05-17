package com.ooredoo.hr.attrition.predictor.service;

import com.ooredoo.hr.attrition.predictor.dto.response.NotificationResponse;
import com.ooredoo.hr.attrition.predictor.entity.Notification;
import com.ooredoo.hr.attrition.predictor.entity.User;
import com.ooredoo.hr.attrition.predictor.enums.ERole;
import com.ooredoo.hr.attrition.predictor.repository.NotificationRepository;
import com.ooredoo.hr.attrition.predictor.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationPanneauService {

    private final NotificationRepository notificationRepository;
    private final UserRepository         userRepository;

    // ─────────────────────────────────────
    // Créer les notifications après le batch
    // Appelé par BatchPredictionService
    // ─────────────────────────────────────
    @Transactional
    public void creerNotificationsApresBatch(
            LocalDateTime dateRecalcul,
            int totalEvalues,
            long critiquesCeMois) {

        String mois = dateRecalcul.getYear() + "-"
                + String.format("%02d", dateRecalcul.getMonthValue());

        String moisLabel = dateRecalcul.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.FRENCH)
                + " " + dateRecalcul.getYear();

        LocalDateTime prochainRecalcul = dateRecalcul.plusMonths(1);

        // Récupérer tous les RH
        List<User> responsablesRH = userRepository.findByUserRole(ERole.RESPONSABLE_RH);

        for (User rh : responsablesRH) {

            // Éviter les doublons si le batch est relancé le même mois
            if (notificationRepository.existsByUserIdAndMoisRecalcul(rh.getId(), mois)) {
                continue;
            }

            notificationRepository.save(Notification.builder()
                    .type("recalcul")
                    .message("Prédiction mensuelle terminée — " + moisLabel)
                    .valeur(String.valueOf(totalEvalues))
                    .lue(false).moisRecalcul(mois).user(rh).build());

            notificationRepository.save(Notification.builder()
                    .type("critique")
                    .message("employés sont passés en risque critique ce mois-ci.")
                    .valeur(String.valueOf(critiquesCeMois))
                    .lue(false).moisRecalcul(mois).user(rh).build());

            notificationRepository.save(Notification.builder()
                    .type("planifie")
                    .message("Prochain recalcul prévu")
                    .valeur(prochainRecalcul.toLocalDate().toString())
                    .lue(false).moisRecalcul(mois).user(rh).build());
        }
    }

    // ─────────────────────────────────────
    // Récupérer les notifications d'un RH
    // ─────────────────────────────────────
    public List<NotificationResponse> getNotifications(Long userId) {
        return notificationRepository
                .findByUserIdOrderByDateCreationDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────
    // Compter les non lues
    // ─────────────────────────────────────
    public long countNonLues(Long userId) {
        return notificationRepository.countByUserIdAndLueFalse(userId);
    }

    // ─────────────────────────────────────
    // Marquer toutes comme lues
    // ─────────────────────────────────────
    @Transactional
    public void marquerToutesLues(Long userId) {
        notificationRepository.marquerToutesLues(userId);
    }

    // ─────────────────────────────────────
    // Mapper
    // ─────────────────────────────────────
    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .message(n.getMessage())
                .valeur(n.getValeur())
                .lue(n.isLue())
                .moisRecalcul(n.getMoisRecalcul())
                .dateCreation(n.getDateCreation())
                .build();
    }
}