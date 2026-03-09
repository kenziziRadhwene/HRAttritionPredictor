package com.ooredoo.hr.attrition.predictor.service;

import com.ooredoo.hr.attrition.predictor.dto.response.AlerteResponse;
import com.ooredoo.hr.attrition.predictor.entity.Alerte;
import com.ooredoo.hr.attrition.predictor.entity.Employee;
import com.ooredoo.hr.attrition.predictor.entity.ScoreRisque;
import com.ooredoo.hr.attrition.predictor.enums.EStatutAlerte;
import com.ooredoo.hr.attrition.predictor.repository.AlerteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlerteService {

    private final AlerteRepository alerteRepository;

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

        Alerte alerte = Alerte.builder()
                .titre(titre)
                .message(message)
                .probabilite(score.getProbabilite())
                .statut(EStatutAlerte.NON_LUE)
                .emailDestinataire("rh@ooredoo.tn")
                .emailEnvoye(false)
                .employee(employee)
                .scoreRisque(score)
                .build();

        return alerteRepository.save(alerte);
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