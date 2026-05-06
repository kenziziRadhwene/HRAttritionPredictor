package com.ooredoo.hr.attrition.predictor.controller;

import com.ooredoo.hr.attrition.predictor.dto.response.BatchPredictionResponse;
import com.ooredoo.hr.attrition.predictor.dto.response.BatchStatusResponse;
import com.ooredoo.hr.attrition.predictor.service.BatchPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/batch-predictions")
@RequiredArgsConstructor
public class BatchPredictionController {

    private final BatchPredictionService batchPredictionService;

    /**
     * POST /api/batch-predictions/all
     * Lance la prédiction ML pour TOUS les employés actifs
     */
    @PostMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE_RH')")
    public ResponseEntity<BatchPredictionResponse> predictAllEmployees() {
        log.info("📡 Requête reçue: prédiction batch pour TOUS les employés");
        BatchPredictionResponse response = batchPredictionService.predictAllEmployees();
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/batch-predictions/department/{department}
     * Lance la prédiction ML pour les employés d'un département spécifique
     */
    @PostMapping("/department/{department}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE_RH')")
    public ResponseEntity<BatchPredictionResponse> predictByDepartment(
            @PathVariable String department) {
        log.info("📡 Requête reçue: prédiction batch pour département: {}", department);
        BatchPredictionResponse response = batchPredictionService.predictByDepartment(department);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/batch-predictions/status
     * Vérifie l'état des prédictions batch
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE_RH')")
    public ResponseEntity<BatchStatusResponse> getBatchStatus() {
        return ResponseEntity.ok(BatchStatusResponse.builder()
                .isRunning(batchPredictionService.isRunning())
                .lastExecutionTime(batchPredictionService.getLastExecutionTime())
                .lastTotalEmployees(batchPredictionService.getLastTotalEmployees())
                .lastSuccessCount(batchPredictionService.getLastSuccessCount())
                .lastFailedCount(batchPredictionService.getLastFailedCount())
                .build());
    }
}