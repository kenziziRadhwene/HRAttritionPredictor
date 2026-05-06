package com.ooredoo.hr.attrition.predictor.service;

import com.ooredoo.hr.attrition.predictor.dto.response.BatchPredictionResponse;
import com.ooredoo.hr.attrition.predictor.entity.Employee;
import com.ooredoo.hr.attrition.predictor.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchPredictionService {

    private final EmployeeRepository employeeRepository;
    private final MLService mlService;

    // Suivi de l'exécution
    private volatile boolean isRunning = false;
    private LocalDateTime lastExecutionTime = null;
    private int lastTotalEmployees = 0;
    private int lastSuccessCount = 0;
    private int lastFailedCount = 0;

    /**
     * Lance la prédiction ML pour tous les employés actifs
     * @return BatchPredictionResponse avec le rapport d'exécution
     */
    @Transactional
    public BatchPredictionResponse predictAllEmployees() {
        // Vérifier si une exécution est déjà en cours
        if (isRunning) {
            log.warn("⚠️ Une prédiction batch est déjà en cours");
            return BatchPredictionResponse.builder()
                    .totalEmployees(0)
                    .successCount(0)
                    .failedCount(0)
                    .errors(List.of("Une prédiction batch est déjà en cours"))
                    .durationMs(0)
                    .build();
        }

        isRunning = true;
        long startTime = System.currentTimeMillis();

        List<Employee> activeEmployees = employeeRepository.findByActiveTrue();
        int total = activeEmployees.size();
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        log.info("🚀 Démarrage de la prédiction batch pour {} employés", total);

        for (Employee employee : activeEmployees) {
            try {
                mlService.predictAndSave(employee.getId());
                success++;

                if (success % 10 == 0) {
                    log.info("📊 Progression batch: {}/{} employés traités", success, total);
                }
            } catch (Exception e) {
                failed++;
                errors.add(String.format("Employé ID %d (%s - %s %s): %s",
                        employee.getId(),
                        employee.getMatricule(),
                        employee.getFirstName(),
                        employee.getLastName(),
                        e.getMessage()));
                log.error("❌ Erreur prédiction pour employé ID {}: {}", employee.getId(), e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        // Mettre à jour les statistiques
        lastExecutionTime = LocalDateTime.now();
        lastTotalEmployees = total;
        lastSuccessCount = success;
        lastFailedCount = failed;
        isRunning = false;

        log.info("✅ Prédiction batch terminée: {} succès, {} échecs, durée: {} ms",
                success, failed, duration);

        return BatchPredictionResponse.builder()
                .totalEmployees(total)
                .successCount(success)
                .failedCount(failed)
                .errors(errors)
                .durationMs(duration)
                .build();
    }

    /**
     * Prédiction batch uniquement pour un département spécifique
     */
    @Transactional
    public BatchPredictionResponse predictByDepartment(String department) {
        long startTime = System.currentTimeMillis();

        List<Employee> employees = employeeRepository.findByDepartmentAndActiveTrue(department);
        int total = employees.size();
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        log.info("🚀 Démarrage prédiction batch pour département '{}' ({} employés)", department, total);

        for (Employee employee : employees) {
            try {
                mlService.predictAndSave(employee.getId());
                success++;
            } catch (Exception e) {
                failed++;
                errors.add(String.format("Employé ID %d (%s): %s",
                        employee.getId(), employee.getMatricule(), e.getMessage()));
                log.error("❌ Erreur prédiction pour employé ID {}: {}", employee.getId(), e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        log.info("✅ Prédiction batch terminée pour département '{}': {} succès, {} échecs",
                department, success, failed);

        return BatchPredictionResponse.builder()
                .totalEmployees(total)
                .successCount(success)
                .failedCount(failed)
                .errors(errors)
                .durationMs(duration)
                .build();
    }

    /**
     * Programme automatique : Prédiction batch tous les jours à 02h00
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledBatchPrediction() {
        log.info("⏰ === PRÉDICTION BATCH AUTOMATIQUE (programmée 02h00) ===");
        BatchPredictionResponse response = predictAllEmployees();
        log.info("📋 Batch programmé terminé: {} succès, {} échecs",
                response.getSuccessCount(), response.getFailedCount());
    }

    /**
     * Retourne le statut de la dernière exécution
     */
    public boolean isRunning() {
        return isRunning;
    }

    public LocalDateTime getLastExecutionTime() {
        return lastExecutionTime;
    }

    public int getLastTotalEmployees() {
        return lastTotalEmployees;
    }

    public int getLastSuccessCount() {
        return lastSuccessCount;
    }

    public int getLastFailedCount() {
        return lastFailedCount;
    }
}