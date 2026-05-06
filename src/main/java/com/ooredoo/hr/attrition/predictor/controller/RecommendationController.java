package com.ooredoo.hr.attrition.predictor.controller;

import com.ooredoo.hr.attrition.predictor.repository.ScoreRisqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final ScoreRisqueRepository scoreRisqueRepository;

    // GET /api/recommendations/employee/{employeeId}
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Map<String, Object>> getRecommendationsByEmployee(
            @PathVariable Long employeeId) {

        return scoreRisqueRepository
                .findTopByEmployeeIdOrderByDateCalculDesc(employeeId)
                .map(score -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("employeeId", score.getEmployee().getId());
                    response.put("probability", score.getProbabilite());
                    response.put("riskLevel", score.getNiveauRisque());

                    String recommandationsStr = score.getRecommandations();
                    List<String> recommendations = new ArrayList<>();

                    if (recommandationsStr != null && !recommandationsStr.isEmpty()) {
                        if (recommandationsStr.contains(",")) {
                            recommendations = Arrays.asList(recommandationsStr.split(","));
                        } else {
                            recommendations = Arrays.asList(recommandationsStr);
                        }
                    } else {
                        recommendations = Arrays.asList(
                                "📈 Suivi mensuel recommandé",
                                "🎓 Évaluation des compétences"
                        );
                    }

                    response.put("recommendations", recommendations);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}