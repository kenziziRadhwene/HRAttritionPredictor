package com.ooredoo.hr.attrition.predictor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ooredoo.hr.attrition.predictor.entity.Employee;
import com.ooredoo.hr.attrition.predictor.entity.ScoreRisque;
import com.ooredoo.hr.attrition.predictor.enums.ENiveauRisque;
import com.ooredoo.hr.attrition.predictor.repository.EmployeeRepository;
import com.ooredoo.hr.attrition.predictor.repository.ScoreRisqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MLService {

    private final EmployeeRepository employeeRepository;
    private final ScoreRisqueRepository scoreRisqueRepository;
    private final AlerteService alerteService;
    private final ObjectMapper objectMapper;

    private static final String ML_API_URL = "http://localhost:8000/api/ml/predict";

    // ─────────────────────────────────────
    // Prédire le risque d'un employé
    // ─────────────────────────────────────
    public ScoreRisque predictAndSave(Long employeeId) {

        // 1. Récupérer l'employé
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé : " + employeeId));

        // 2. Construire le payload pour FastAPI
        Map<String, Object> payload = buildPayload(employee);

        // 3. Appeler FastAPI
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                ML_API_URL, request, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null)
            throw new RuntimeException("Erreur lors de l'appel au microservice ML");

        Map<String, Object> result = response.getBody();

        // 4. Parser la réponse
        Double probabilite    = ((Number) result.get("probability")).doubleValue();
        String niveauRisqueStr = (String) result.get("risk_level");
        Double seuilUtilise   = ((Number) result.get("threshold_used")).doubleValue();

        // 5. Convertir le niveau de risque
        ENiveauRisque niveauRisque = switch (niveauRisqueStr) {
            case "ÉLEVÉ"  -> ENiveauRisque.ÉLEVÉ;
            case "MOYEN"  -> ENiveauRisque.MOYEN;
            default       -> ENiveauRisque.FAIBLE;
        };

        // 6. Sérialiser les facteurs SHAP et recommandations en JSON
        String facteursPrincipaux;
        String recommandations;
        try {
            facteursPrincipaux = objectMapper.writeValueAsString(
                    result.get("top_risk_factors"));
            recommandations = objectMapper.writeValueAsString(
                    result.get("recommendation_factors"));
        } catch (Exception e) {
            facteursPrincipaux = "[]";
            recommandations    = "[]";
        }

        // 7. Sauvegarder le score en base
        ScoreRisque score = ScoreRisque.builder()
                .employee(employee)
                .probabilite(probabilite)
                .niveauRisque(niveauRisque)
                .seuilUtilise(seuilUtilise)
                .facteursPrincipaux(facteursPrincipaux)
                .recommandations(recommandations)
                .modelVersion("2.0-optimized")
                .build();

        ScoreRisque saved = scoreRisqueRepository.save(score);

        // 8. Générer une alerte si risque MOYEN ou ÉLEVÉ
        if (niveauRisque == ENiveauRisque.MOYEN || niveauRisque == ENiveauRisque.ÉLEVÉ) {
            alerteService.createAlerte(employee, saved);
        }

        return saved;
    }

    // ─────────────────────────────────────
    // Construire le payload FastAPI
    // Mapping Employee → Format IBM HR
    // ─────────────────────────────────────
    private Map<String, Object> buildPayload(Employee employee) {
        Map<String, Object> payload = new HashMap<>();

        // Données personnelles
        payload.put("Age",              employee.getAge());
        payload.put("Gender",           employee.getGender().name());
        payload.put("MaritalStatus",    employee.getMaritalStatus().name());
        payload.put("DistanceFromHome", employee.getDistanceFromHome());
        payload.put("Education",        employee.getEducation());
        payload.put("EducationField",   employee.getEducationField());

        // Données professionnelles
        // ⚠️ Le mapping Ooredoo → IBM est géré dans predictor.py
        payload.put("Department",    employee.getDepartment().name());
        payload.put("JobRole",       employee.getJobRole().name());
        payload.put("JobLevel",      employee.getJobLevel());
        payload.put("BusinessTravel",employee.getBusinessTravel());

        // Rémunération
        payload.put("MonthlyIncome",    employee.getMonthlyIncome());
        payload.put("DailyRate",        employee.getDailyRate());
        payload.put("HourlyRate",       employee.getHourlyRate());
        payload.put("MonthlyRate",      employee.getMonthlyRate());
        payload.put("PercentSalaryHike",employee.getPercentSalaryHike());
        payload.put("StockOptionLevel", employee.getStockOptionLevel());

        // Satisfaction et engagement
        payload.put("JobSatisfaction",          employee.getJobSatisfaction());
        payload.put("EnvironmentSatisfaction",  employee.getEnvironmentSatisfaction());
        payload.put("RelationshipSatisfaction", employee.getRelationshipSatisfaction());
        payload.put("JobInvolvement",           employee.getJobInvolvement());
        payload.put("WorkLifeBalance",          employee.getWorkLifeBalance());
        payload.put("PerformanceRating",        employee.getPerformanceRating());

        // Activité
        payload.put("OverTime", employee.getOverTime() ? "Yes" : "No");
        payload.put("NumCompaniesWorked",    employee.getNumCompaniesWorked());
        payload.put("TotalWorkingYears",     employee.getTotalWorkingYears());
        payload.put("YearsAtCompany",        employee.getYearsAtCompany());
        payload.put("YearsInCurrentRole",    employee.getYearsInCurrentRole());
        payload.put("YearsSinceLastPromotion",employee.getYearsSinceLastPromotion());
        payload.put("YearsWithCurrManager",  employee.getYearsWithCurrManager());
        payload.put("TrainingTimesLastYear", employee.getTrainingTimesLastYear());

        return payload;
    }
}