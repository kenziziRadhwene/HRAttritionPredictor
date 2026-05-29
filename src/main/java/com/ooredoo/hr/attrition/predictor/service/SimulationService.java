package com.ooredoo.hr.attrition.predictor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ooredoo.hr.attrition.predictor.dto.request.SimulationFormationRequest;
import com.ooredoo.hr.attrition.predictor.dto.request.SimulationPosteRequest;
import com.ooredoo.hr.attrition.predictor.dto.request.SimulationSalaireRequest;
import com.ooredoo.hr.attrition.predictor.dto.response.ComparaisonSimulationResponse;
import com.ooredoo.hr.attrition.predictor.dto.response.SimulationResponse;
import com.ooredoo.hr.attrition.predictor.entity.Employee;
import com.ooredoo.hr.attrition.predictor.entity.ScoreRisque;
import com.ooredoo.hr.attrition.predictor.enums.EAuditAction;
import com.ooredoo.hr.attrition.predictor.enums.ENiveauRisque;
import com.ooredoo.hr.attrition.predictor.repository.EmployeeRepository;
import com.ooredoo.hr.attrition.predictor.repository.ScoreRisqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final EmployeeRepository employeeRepository;
    private final ScoreRisqueRepository scoreRisqueRepository;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    private static final String ML_URL = "http://localhost:8000/api/ml/predict";

    // ─────────────────────────────────────
    // US12 — Simuler impact augmentation salaire
    // ─────────────────────────────────────
    public SimulationResponse simulerAugmentationSalaire(
            SimulationSalaireRequest request) {

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException(
                        "Employé non trouvé : " + request.getEmployeeId()));

        ScoreRisque scoreActuel = scoreRisqueRepository
                .findTopByEmployeeIdOrderByDateCalculDesc(employee.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Aucun score trouvé pour cet employé"));

        Double probabiliteActuelle = scoreActuel.getProbabilite();
        Double ancienSalaire = employee.getMonthlyIncome().doubleValue();
        Double nouveauSalaire = ancienSalaire
                * (1 + request.getPourcentageAugmentation() / 100);

        Map<String, Object> payload = buildMLPayload(employee);
        payload.put("MonthlyIncome", nouveauSalaire.intValue());

        Double probabiliteSimulee = callMLService(payload);
        Double impact = probabiliteActuelle - probabiliteSimulee;

        auditLogService.log(
                EAuditAction.SIMULATION_SALAIRE,
                "employees",
                employee.getId(),
                String.format(
                        "{\"matricule\": \"%s\", \"augmentation\": %.0f%%, " +
                                "\"probabiliteAvant\": %.2f, \"probabiliteApres\": %.2f}",
                        employee.getMatricule(),
                        request.getPourcentageAugmentation(),
                        probabiliteActuelle,
                        probabiliteSimulee)
        );

        return SimulationResponse.builder()
                .employeeId(employee.getId())
                .employeeNom(employee.getFirstName() + " " + employee.getLastName())
                .employeeMatricule(employee.getMatricule())
                .typeSimulation("AUGMENTATION_SALAIRE")
                .descriptionScenario(String.format(
                        "Augmentation de %.0f%% : %d TND → %d TND",
                        request.getPourcentageAugmentation(),
                        ancienSalaire.intValue(),
                        nouveauSalaire.intValue()))
                .probabiliteActuelle(probabiliteActuelle)
                .probabiliteSimulee(probabiliteSimulee)
                .impactPourcentage(impact)
                .niveauRisqueActuel(calculerNiveau(probabiliteActuelle).name())
                .niveauRisqueSimule(calculerNiveau(probabiliteSimulee).name())
                .recommandation(genererRecommandation(impact,
                        request.getPourcentageAugmentation()))
                .actionRecommandee(impact > 0.05)
                .build();
    }

    // ─────────────────────────────────────
    // US13 — Simuler impact changement de poste
    // ─────────────────────────────────────
    public SimulationResponse simulerChangementPoste(
            SimulationPosteRequest request) {

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException(
                        "Employé non trouvé : " + request.getEmployeeId()));

        ScoreRisque scoreActuel = scoreRisqueRepository
                .findTopByEmployeeIdOrderByDateCalculDesc(employee.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Aucun score trouvé pour cet employé"));

        Double probabiliteActuelle = scoreActuel.getProbabilite();

        Map<String, Object> payload = buildMLPayload(employee);
        payload.put("JobRole",    request.getNouveauPoste().name());
        payload.put("Department", request.getNouveauDepartement().name());
        if (request.getNouveauJobLevel() != null) {
            payload.put("JobLevel", request.getNouveauJobLevel());
        }

        Double probabiliteSimulee = callMLService(payload);
        Double impact = probabiliteActuelle - probabiliteSimulee;

        auditLogService.log(
                EAuditAction.SIMULATION_POSTE,
                "employees",
                employee.getId(),
                String.format(
                        "{\"matricule\": \"%s\", \"ancienPoste\": \"%s\", " +
                                "\"nouveauPoste\": \"%s\", \"probabiliteAvant\": %.2f, " +
                                "\"probabiliteApres\": %.2f}",
                        employee.getMatricule(),
                        employee.getJobRole().name(),
                        request.getNouveauPoste().name(),
                        probabiliteActuelle,
                        probabiliteSimulee)
        );

        String scenario = String.format("%s → %s (%s)",
                employee.getJobRole().name(),
                request.getNouveauPoste().name(),
                request.getNouveauDepartement().name());

        return SimulationResponse.builder()
                .employeeId(employee.getId())
                .employeeNom(employee.getFirstName() + " " + employee.getLastName())
                .employeeMatricule(employee.getMatricule())
                .typeSimulation("CHANGEMENT_POSTE")
                .descriptionScenario(scenario)
                .probabiliteActuelle(probabiliteActuelle)
                .probabiliteSimulee(probabiliteSimulee)
                .impactPourcentage(impact)
                .niveauRisqueActuel(calculerNiveau(probabiliteActuelle).name())
                .niveauRisqueSimule(calculerNiveau(probabiliteSimulee).name())
                .recommandation(genererRecommandationPoste(impact,
                        request.getNouveauPoste().name()))
                .actionRecommandee(impact > 0.05)
                .build();
    }

    // ─────────────────────────────────────
    // US14 — Simuler impact formation
    // ─────────────────────────────────────
    public SimulationResponse simulerFormation(
            SimulationFormationRequest request) {

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException(
                        "Employé non trouvé : " + request.getEmployeeId()));

        ScoreRisque scoreActuel = scoreRisqueRepository
                .findTopByEmployeeIdOrderByDateCalculDesc(employee.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Aucun score trouvé pour cet employé"));

        Double probabiliteActuelle = scoreActuel.getProbabilite();
        Integer anciennesFormations = employee.getTrainingTimesLastYear();
        Integer nouvellesFormations = anciennesFormations
                + request.getNombreFormations();

        Map<String, Object> payload = buildMLPayload(employee);
        payload.put("TrainingTimesLastYear", nouvellesFormations);

        Double probabiliteSimulee = callMLService(payload);
        Double impact = probabiliteActuelle - probabiliteSimulee;

        auditLogService.log(
                EAuditAction.SIMULATION_FORMATION,
                "employees",
                employee.getId(),
                String.format(
                        "{\"matricule\": \"%s\", \"formationsAjoutees\": %d, " +
                                "\"probabiliteAvant\": %.2f, \"probabiliteApres\": %.2f}",
                        employee.getMatricule(),
                        request.getNombreFormations(),
                        probabiliteActuelle,
                        probabiliteSimulee)
        );

        return SimulationResponse.builder()
                .employeeId(employee.getId())
                .employeeNom(employee.getFirstName() + " " + employee.getLastName())
                .employeeMatricule(employee.getMatricule())
                .typeSimulation("FORMATION")
                .descriptionScenario(String.format(
                        "Formations : %d → %d (+%d formation(s) cette année)",
                        anciennesFormations,
                        nouvellesFormations,
                        request.getNombreFormations()))
                .probabiliteActuelle(probabiliteActuelle)
                .probabiliteSimulee(probabiliteSimulee)
                .impactPourcentage(impact)
                .niveauRisqueActuel(calculerNiveau(probabiliteActuelle).name())
                .niveauRisqueSimule(calculerNiveau(probabiliteSimulee).name())
                .recommandation(genererRecommandationFormation(impact,
                        request.getNombreFormations()))
                .actionRecommandee(impact > 0.05)
                .build();
    }

    // ─────────────────────────────────────
    // US15 — Comparer tous les scénarios
    // ─────────────────────────────────────
    public ComparaisonSimulationResponse comparerScenarios(Long employeeId) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException(
                        "Employé non trouvé : " + employeeId));

        ScoreRisque scoreActuel = scoreRisqueRepository
                .findTopByEmployeeIdOrderByDateCalculDesc(employeeId)
                .orElseThrow(() -> new RuntimeException("Aucun score trouvé"));

        Double probabiliteActuelle = scoreActuel.getProbabilite();

        SimulationSalaireRequest salaireReq10 = new SimulationSalaireRequest();
        salaireReq10.setEmployeeId(employeeId);
        salaireReq10.setPourcentageAugmentation(10.0);
        SimulationResponse simSalaire10 = simulerAugmentationSalaire(salaireReq10);

        SimulationSalaireRequest salaireReq20 = new SimulationSalaireRequest();
        salaireReq20.setEmployeeId(employeeId);
        salaireReq20.setPourcentageAugmentation(20.0);
        SimulationResponse simSalaire20 = simulerAugmentationSalaire(salaireReq20);

        SimulationSalaireRequest salaireReq30 = new SimulationSalaireRequest();
        salaireReq30.setEmployeeId(employeeId);
        salaireReq30.setPourcentageAugmentation(30.0);
        SimulationResponse simSalaire30 = simulerAugmentationSalaire(salaireReq30);

        SimulationFormationRequest formationReq = new SimulationFormationRequest();
        formationReq.setEmployeeId(employeeId);
        formationReq.setNombreFormations(3);
        SimulationResponse simFormation = simulerFormation(formationReq);

        SimulationPosteRequest posteReq = new SimulationPosteRequest();
        posteReq.setEmployeeId(employeeId);
        posteReq.setNouveauPoste(employee.getJobRole());
        posteReq.setNouveauDepartement(employee.getDepartment());
        posteReq.setNouveauJobLevel(employee.getJobLevel() + 1);
        SimulationResponse simPoste = simulerChangementPoste(posteReq);

        SimulationResponse meilleure = simSalaire10;
        if (simSalaire20.getImpactPourcentage() > meilleure.getImpactPourcentage())
            meilleure = simSalaire20;
        if (simSalaire30.getImpactPourcentage() > meilleure.getImpactPourcentage())
            meilleure = simSalaire30;
        if (simFormation.getImpactPourcentage() > meilleure.getImpactPourcentage())
            meilleure = simFormation;
        if (simPoste.getImpactPourcentage() > meilleure.getImpactPourcentage())
            meilleure = simPoste;

        return ComparaisonSimulationResponse.builder()
                .employeeId(employee.getId())
                .employeeNom(employee.getFirstName() + " " + employee.getLastName())
                .employeeMatricule(employee.getMatricule())
                .probabiliteActuelle(probabiliteActuelle)
                .niveauRisqueActuel(calculerNiveau(probabiliteActuelle).name())
                .simulationSalaire10(simSalaire10)
                .simulationSalaire20(simSalaire20)
                .simulationSalaire30(simSalaire30)
                .simulationPoste(simPoste)
                .simulationFormation(simFormation)
                .meilleureAction(meilleure.getRecommandation())
                .typeSimulationRecommande(meilleure.getTypeSimulation())
                .meilleurImpact(meilleure.getImpactPourcentage())
                .build();
    }

    // ─────────────────────────────────────
    // Construire le payload ML
    // ─────────────────────────────────────
    private Map<String, Object> buildMLPayload(Employee employee) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("Age",                      employee.getAge());
        payload.put("BusinessTravel",           employee.getBusinessTravel());
        payload.put("DailyRate",                employee.getDailyRate());
        payload.put("Department",               employee.getDepartment().name());
        payload.put("DistanceFromHome",         employee.getDistanceFromHome());
        payload.put("Education",                employee.getEducation());
        payload.put("EducationField",           employee.getEducationField());
        payload.put("EnvironmentSatisfaction",  employee.getEnvironmentSatisfaction());
        payload.put("Gender",                   employee.getGender().name());
        payload.put("HourlyRate",               employee.getHourlyRate());
        payload.put("JobInvolvement",           employee.getJobInvolvement());
        payload.put("JobLevel",                 employee.getJobLevel());
        payload.put("JobRole",                  employee.getJobRole().name());
        payload.put("JobSatisfaction",          employee.getJobSatisfaction());
        payload.put("MaritalStatus",            employee.getMaritalStatus().name());
        payload.put("MonthlyIncome",            employee.getMonthlyIncome());
        payload.put("MonthlyRate",              employee.getMonthlyRate());
        payload.put("NumCompaniesWorked",       employee.getNumCompaniesWorked());
        payload.put("OverTime",                 employee.getOverTime() ? "Yes" : "No");
        payload.put("PercentSalaryHike",        employee.getPercentSalaryHike());
        payload.put("PerformanceRating",        employee.getPerformanceRating());
        payload.put("RelationshipSatisfaction", employee.getRelationshipSatisfaction());
        payload.put("StockOptionLevel",         employee.getStockOptionLevel());
        payload.put("TotalWorkingYears",        employee.getTotalWorkingYears());
        payload.put("TrainingTimesLastYear",    employee.getTrainingTimesLastYear());
        payload.put("WorkLifeBalance",          employee.getWorkLifeBalance());
        payload.put("YearsAtCompany",           employee.getYearsAtCompany());
        payload.put("YearsInCurrentRole",       employee.getYearsInCurrentRole());
        payload.put("YearsSinceLastPromotion",  employee.getYearsSinceLastPromotion());
        payload.put("YearsWithCurrManager",     employee.getYearsWithCurrManager());
        return payload;
    }

    // ─────────────────────────────────────
    // Appeler FastAPI ML
    // ─────────────────────────────────────
    private Double callMLService(Map<String, Object> payload) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            Map response = restTemplate.postForObject(ML_URL, payload, Map.class);
            return ((Number) response.get("probability")).doubleValue();
        } catch (Exception e) {
            throw new RuntimeException("Erreur appel ML : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────
    // Calculer le niveau de risque
    // ─────────────────────────────────────
    private ENiveauRisque calculerNiveau(Double probabilite) {
        if (probabilite >= 0.85) return ENiveauRisque.ÉLEVÉ;
        if (probabilite >= 0.76) return ENiveauRisque.MOYEN;
        return ENiveauRisque.FAIBLE;
    }

    // ─────────────────────────────────────
    // Recommandations
    // ─────────────────────────────────────
    private String genererRecommandation(Double impact, Double pourcentage) {
        if (impact > 0.20)
            return String.format(
                    "✅ Très efficace ! Une augmentation de %.0f%% réduit significativement le risque.",
                    pourcentage);
        else if (impact > 0.05)
            return String.format(
                    "👍 Efficace. Une augmentation de %.0f%% améliore la rétention.",
                    pourcentage);
        else if (impact > 0)
            return String.format(
                    "⚠️ Impact limité. Une augmentation de %.0f%% a peu d'effet.",
                    pourcentage);
        else
            return "❌ Inefficace. L'augmentation salariale n'est pas le levier adapté.";
    }

    private String genererRecommandationPoste(Double impact, String nouveauPoste) {
        if (impact > 0.20)
            return String.format(
                    "✅ Très efficace ! Le transfert vers %s réduit significativement le risque.",
                    nouveauPoste);
        else if (impact > 0.05)
            return String.format(
                    "👍 Efficace. Le changement vers %s améliore la rétention.",
                    nouveauPoste);
        else if (impact > 0)
            return String.format(
                    "⚠️ Impact limité. Le changement vers %s a peu d'effet.",
                    nouveauPoste);
        else
            return "❌ Inefficace. Le changement de poste n'est pas le levier adapté.";
    }

    private String genererRecommandationFormation(Double impact,
                                                  Integer nombreFormations) {
        if (impact > 0.20)
            return String.format(
                    "✅ Très efficace ! %d formation(s) réduit significativement le risque.",
                    nombreFormations);
        else if (impact > 0.05)
            return String.format(
                    "👍 Efficace. %d formation(s) améliore la rétention.",
                    nombreFormations);
        else if (impact > 0)
            return String.format(
                    "⚠️ Impact limité. %d formation(s) a peu d'effet.",
                    nombreFormations);
        else
            return "❌ Inefficace. La formation n'est pas le levier adapté.";
    }
}