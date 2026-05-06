package com.ooredoo.hr.attrition.predictor.service;

import com.ooredoo.hr.attrition.predictor.dto.response.AlerteResponse;
import com.ooredoo.hr.attrition.predictor.entity.Alerte;
import com.ooredoo.hr.attrition.predictor.entity.Employee;
import com.ooredoo.hr.attrition.predictor.entity.ScoreRisque;
import com.ooredoo.hr.attrition.predictor.enums.EStatutAlerte;
import com.ooredoo.hr.attrition.predictor.repository.AlerteRepository;
import com.ooredoo.hr.attrition.predictor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlerteService {

    private final AlerteRepository alerteRepository;
    private final UserRepository userRepository;  // ⭐ AJOUTÉ

    private static final String NOTIFICATION_URL =
            "http://localhost:8082/api/notifications/alerte";

    /**
     * Récupère la liste des emails de tous les responsables RH
     */
    private String getResponsablesRHEmails() {
        List<String> emails = userRepository.findAllResponsableRHEmails();

        if (emails == null || emails.isEmpty()) {
            System.out.println("⚠️ Aucun RH trouvé, utilisation du fallback");
            return "hr.attrition.ooredoo@gmail.com";
        }

        String result = String.join(",", emails);
        System.out.println("📧 Emails des RH destinataires : " + result);
        return result;
    }

    // ─────────────────────────────────────
    // Créer une alerte automatiquement
    // ─────────────────────────────────────
    public Alerte createAlerte(Employee employee, ScoreRisque score) {

        String niveau = score.getNiveauRisque().name();
        String titre  = String.format("⚠️ Risque %s détecté — %s %s",
                niveau,
                employee.getFirstName(),
                employee.getLastName());

        String message = String.format(
                "L'employé %s %s (Matricule: %s, Département: %s) " +
                        "présente un risque de départ %s avec une probabilité de %.1f%%.\n" +
                        "Une intervention RH est recommandée.",
                employee.getFirstName(),
                employee.getLastName(),
                employee.getMatricule(),
                employee.getDepartment().name(),
                niveau,
                score.getProbabilite() * 100
        );

        // ⭐ Récupérer tous les emails des responsables RH
        String destinataires = getResponsablesRHEmails();

        Alerte alerte = Alerte.builder()
                .titre(titre)
                .message(message)
                .probabilite(score.getProbabilite())
                .statut(EStatutAlerte.NON_LUE)
                .emailDestinataire(destinataires)  // ⭐ MODIFIÉ
                .emailEnvoye(false)
                .employee(employee)
                .scoreRisque(score)
                .build();

        Alerte saved = alerteRepository.save(alerte);

        // ─────────────────────────────────────
        // Appel au microservice de notification
        // ─────────────────────────────────────
        try {
            Map<String, Object> mailRequest = new HashMap<>();
            mailRequest.put("employeeId",          employee.getId());
            mailRequest.put("employeeNom",         employee.getFirstName() + " " + employee.getLastName());
            mailRequest.put("employeeMatricule",   employee.getMatricule());
            mailRequest.put("employeeDepartement", employee.getDepartment().name());
            mailRequest.put("employeePoste",       employee.getJobRole().name());
            mailRequest.put("employeeAnciennete",  employee.getYearsAtCompany());
            mailRequest.put("probabilite",         score.getProbabilite());
            mailRequest.put("niveauRisque",        score.getNiveauRisque().name());
            mailRequest.put("seuilUtilise",        score.getSeuilUtilise());
            mailRequest.put("dateCalcul",          score.getDateCalcul()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            mailRequest.put("modelVersion",        score.getModelVersion());
            mailRequest.put("emailDestinataire",   destinataires);  // ⭐ MODIFIÉ

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    NOTIFICATION_URL, mailRequest, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                saved.setEmailEnvoye(true);
                alerteRepository.save(saved);
                System.out.println("✅ Notification envoyée aux RH : " + destinataires);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur notification : " + e.getMessage());
        }

        return saved;
    }

    // ─────────────────────────────────────
    // Récupérer toutes les alertes
    // ─────────────────────────────────────
    public List<AlerteResponse> getAllAlertes() {
        return alerteRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────
    // Récupérer les alertes non lues
    // ─────────────────────────────────────
    public List<AlerteResponse> getAlertesNonLues() {
        return alerteRepository.findByStatut(EStatutAlerte.NON_LUE)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────
    // Marquer une alerte comme lue
    // ─────────────────────────────────────
    public AlerteResponse marquerCommeLue(Long id) {
        Alerte alerte = alerteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerte non trouvée : " + id));
        alerte.setStatut(EStatutAlerte.LUE);
        return toResponse(alerteRepository.save(alerte));
    }

    // ─────────────────────────────────────
    // Marquer une alerte comme traitée
    // ─────────────────────────────────────
    public AlerteResponse marquerCommeTraitee(Long id) {
        Alerte alerte = alerteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerte non trouvée : " + id));
        alerte.setStatut(EStatutAlerte.TRAITEE);
        return toResponse(alerteRepository.save(alerte));
    }

    // ─────────────────────────────────────
    // Mapper Entity → DTO
    // ─────────────────────────────────────
    private AlerteResponse toResponse(Alerte alerte) {
        return AlerteResponse.builder()
                .id(alerte.getId())
                .titre(alerte.getTitre())
                .message(alerte.getMessage())
                .probabilite(alerte.getProbabilite())
                .statut(alerte.getStatut())
                .emailDestinataire(alerte.getEmailDestinataire())
                .emailEnvoye(alerte.getEmailEnvoye())
                .dateEnvoi(alerte.getDateEnvoi())
                .dateCreation(alerte.getDateCreation())
                .employeeId(alerte.getEmployee().getId())
                .employeeNom(alerte.getEmployee().getFirstName()
                        + " " + alerte.getEmployee().getLastName())
                .employeeMatricule(alerte.getEmployee().getMatricule())
                .build();
    }
}