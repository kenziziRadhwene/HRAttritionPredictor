package com.ooredoo.hr.attrition.predictor.controller;

import com.ooredoo.hr.attrition.predictor.dto.response.ScoreRisqueResponse;
import com.ooredoo.hr.attrition.predictor.entity.Employee;
import com.ooredoo.hr.attrition.predictor.entity.ScoreRisque;
import com.ooredoo.hr.attrition.predictor.enums.ENiveauRisque;
import com.ooredoo.hr.attrition.predictor.repository.EmployeeRepository;
import com.ooredoo.hr.attrition.predictor.repository.ScoreRisqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreRisqueController {

    private final ScoreRisqueRepository scoreRisqueRepository;
    private final EmployeeRepository employeeRepository;

    // ─────────────────────────────────────
    // GET /api/scores/employee/{employeeId}
    // Historique des scores d'un employé
    // ─────────────────────────────────────
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<ScoreRisqueResponse>> getScoresByEmployee(
            @PathVariable Long employeeId) {

        List<ScoreRisqueResponse> scores = scoreRisqueRepository
                .findByEmployeeIdOrderByDateCalculDesc(employeeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(scores);
    }

    // ─────────────────────────────────────
    // GET /api/scores/employee/{employeeId}/latest
    // Dernier score d'un employé
    // ─────────────────────────────────────
    @GetMapping("/employee/{employeeId}/latest")
    public ResponseEntity<ScoreRisqueResponse> getLatestScore(
            @PathVariable Long employeeId) {

        return scoreRisqueRepository
                .findTopByEmployeeIdOrderByDateCalculDesc(employeeId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────
    // Mapper Entity → DTO
    // ─────────────────────────────────────
    private ScoreRisqueResponse toResponse(ScoreRisque score) {
        return ScoreRisqueResponse.builder()
                .id(score.getId())
                .employeeId(score.getEmployee().getId())
                .employeeNom(score.getEmployee().getFirstName()
                        + " " + score.getEmployee().getLastName())
                .employeeMatricule(score.getEmployee().getMatricule())
                .probabilite(score.getProbabilite())
                .niveauRisque(score.getNiveauRisque())
                .seuilUtilise(score.getSeuilUtilise())
                .facteursPrincipaux(score.getFacteursPrincipaux())
                .recommandations(score.getRecommandations())
                .dateCalcul(score.getDateCalcul())
                .modelVersion(score.getModelVersion())
                .build();
    }



    // GET /api/scores/recommandations
// Tous les derniers scores avec recommandations
    @GetMapping("/recommandations")
    public ResponseEntity<List<ScoreRisqueResponse>> getRecommandations() {
        List<Employee> actifs = employeeRepository.findByActiveTrue();
        List<ScoreRisqueResponse> result = actifs.stream()
                .map(e -> scoreRisqueRepository
                        .findTopByEmployeeIdOrderByDateCalculDesc(e.getId())
                        .orElse(null))
                .filter(Objects::nonNull)
                .filter(s -> s.getNiveauRisque() != ENiveauRisque.FAIBLE)
                .sorted(Comparator.comparingDouble(ScoreRisque::getProbabilite).reversed())
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}