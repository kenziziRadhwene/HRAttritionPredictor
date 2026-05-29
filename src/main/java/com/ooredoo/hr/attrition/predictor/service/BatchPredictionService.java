package com.ooredoo.hr.attrition.predictor.service;

import com.ooredoo.hr.attrition.predictor.client.NotificationClient;
import com.ooredoo.hr.attrition.predictor.client.NotificationClient.EmployeeRiskPayload;
import com.ooredoo.hr.attrition.predictor.dto.response.BatchPredictionResponse;
import com.ooredoo.hr.attrition.predictor.entity.Employee;
import com.ooredoo.hr.attrition.predictor.entity.ScoreRisque;
import com.ooredoo.hr.attrition.predictor.enums.EAuditAction;
import com.ooredoo.hr.attrition.predictor.enums.ENiveauRisque;
import com.ooredoo.hr.attrition.predictor.repository.EmployeeRepository;
import com.ooredoo.hr.attrition.predictor.repository.ScoreRisqueRepository;
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
    private final NotificationClient notificationClient;
    private final NotificationPanneauService notificationPanneauService;
    private final ScoreRisqueRepository scoreRisqueRepository;
    private final AuditLogService auditLogService;

    private volatile boolean isRunning = false;
    private LocalDateTime lastExecutionTime = null;
    private int lastTotalEmployees = 0;
    private int lastSuccessCount = 0;
    private int lastFailedCount = 0;

    @Transactional
    public BatchPredictionResponse predictAllEmployees() {
        if (isRunning) {
            log.warn("⚠️ Une prédiction batch est déjà en cours");
            return BatchPredictionResponse.builder()
                    .totalEmployees(0).successCount(0).failedCount(0)
                    .errors(List.of("Une prédiction batch est déjà en cours"))
                    .durationMs(0).build();
        }

        isRunning = true;
        long startTime = System.currentTimeMillis();

        List<Employee> activeEmployees = employeeRepository.findByActiveTrue();
        int total = activeEmployees.size();
        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        log.info("🚀 Démarrage de la prédiction batch pour {} employés", total);

        for (Employee employee : activeEmployees) {
            try {
                mlService.predictAndSave(employee.getId());
                success++;
                if (success % 10 == 0)
                    log.info("📊 Progression batch: {}/{}", success, total);
            } catch (Exception e) {
                failed++;
                errors.add(String.format("Employé ID %d (%s - %s %s): %s",
                        employee.getId(), employee.getMatricule(),
                        employee.getFirstName(), employee.getLastName(),
                        e.getMessage()));
                log.error("❌ Erreur prédiction ID {}: {}", employee.getId(),
                        e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        lastExecutionTime = LocalDateTime.now();
        lastTotalEmployees = total;
        lastSuccessCount = success;
        lastFailedCount = failed;
        isRunning = false;

        log.info("✅ Batch terminé: {} succès, {} échecs, {} ms",
                success, failed, duration);

        LocalDateTime maintenant = LocalDateTime.now();
        long critiquesCeMois = scoreRisqueRepository
                .countByNiveauRisqueAndDateCalculAfter(
                        ENiveauRisque.ÉLEVÉ, maintenant.minusMinutes(30));

        notificationPanneauService.creerNotificationsApresBatch(
                maintenant, success, critiquesCeMois);

        auditLogService.log(
                EAuditAction.PREDICTION_BATCH,
                "scores_risque",
                null,
                String.format("{\"total\": %d, \"succes\": %d, \"echecs\": %d}",
                        total, success, failed)
        );

        sendRiskReportToHR();

        return BatchPredictionResponse.builder()
                .totalEmployees(total).successCount(success).failedCount(failed)
                .errors(errors).durationMs(duration).build();
    }

    @Transactional
    public BatchPredictionResponse predictByDepartment(String department) {
        long startTime = System.currentTimeMillis();

        List<Employee> employees = employeeRepository
                .findByDepartmentAndActiveTrue(department);
        int total = employees.size();
        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        log.info("🚀 Batch département '{}' ({} employés)", department, total);

        for (Employee employee : employees) {
            try {
                mlService.predictAndSave(employee.getId());
                success++;
            } catch (Exception e) {
                failed++;
                errors.add(String.format("Employé ID %d (%s): %s",
                        employee.getId(), employee.getMatricule(), e.getMessage()));
                log.error("❌ Erreur prédiction ID {}: {}", employee.getId(),
                        e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Batch '{}' terminé: {} succès, {} échecs",
                department, success, failed);

        auditLogService.log(
                EAuditAction.PREDICTION_BATCH_DEPARTEMENT,
                "scores_risque",
                null,
                String.format(
                        "{\"departement\": \"%s\", \"total\": %d, \"succes\": %d}",
                        department, total, success)
        );

        return BatchPredictionResponse.builder()
                .totalEmployees(total).successCount(success).failedCount(failed)
                .errors(errors).durationMs(duration).build();
    }

    @Scheduled(cron = "0 0 8 1 * ?")
    public void scheduledBatchPrediction() {
        log.info("⏰ === PRÉDICTION BATCH AUTOMATIQUE ===");
        BatchPredictionResponse response = predictAllEmployees();
        log.info("📋 Batch programmé: {} succès, {} échecs",
                response.getSuccessCount(), response.getFailedCount());
    }

    private void sendRiskReportToHR() {
        try {
            List<Employee> actifs = employeeRepository.findByActiveTrue();
            List<EmployeeRiskPayload> highRiskEmployees = actifs.stream()
                    .map(emp -> scoreRisqueRepository
                            .findTopByEmployeeIdOrderByDateCalculDesc(emp.getId())
                            .orElse(null))
                    .filter(score -> score != null)
                    .filter(score -> ENiveauRisque.ÉLEVÉ == score.getNiveauRisque())
                    .map(score -> {
                        Employee emp = score.getEmployee();
                        return new EmployeeRiskPayload(
                                emp.getId(),
                                emp.getFirstName() + " " + emp.getLastName(),
                                emp.getMatricule(),
                                emp.getDepartment().name(),
                                emp.getJobRole().name(),
                                emp.getYearsAtCompany(),
                                score.getProbabilite(),
                                score.getNiveauRisque().name(),
                                score.getDateCalcul().toString(),
                                score.getModelVersion()
                        );
                    })
                    .toList();

            if (highRiskEmployees.isEmpty()) {
                log.info("ℹ️ Aucun employé à risque élevé");
                return;
            }

            log.info("📨 Envoi rapport PDF: {} employés", highRiskEmployees.size());
            notificationClient.sendRiskReport(highRiskEmployees);

        } catch (Exception e) {
            log.error("❌ Erreur rapport RH: {}", e.getMessage());
        }
    }

    public boolean isRunning()                  { return isRunning; }
    public LocalDateTime getLastExecutionTime() { return lastExecutionTime; }
    public int getLastTotalEmployees()          { return lastTotalEmployees; }
    public int getLastSuccessCount()            { return lastSuccessCount; }
    public int getLastFailedCount()             { return lastFailedCount; }
}