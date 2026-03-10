package com.ooredoo.hr.attrition.predictor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ooredoo.hr.attrition.predictor.dto.request.SimulationFormationRequest;
import com.ooredoo.hr.attrition.predictor.dto.request.SimulationSalaireRequest;
import com.ooredoo.hr.attrition.predictor.dto.response.ComparaisonSimulationResponse;
import com.ooredoo.hr.attrition.predictor.dto.response.SimulationResponse;
import com.ooredoo.hr.attrition.predictor.entity.Employee;
import com.ooredoo.hr.attrition.predictor.entity.ScoreRisque;
import com.ooredoo.hr.attrition.predictor.enums.ENiveauRisque;
import com.ooredoo.hr.attrition.predictor.repository.EmployeeRepository;
import com.ooredoo.hr.attrition.predictor.repository.ScoreRisqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.ooredoo.hr.attrition.predictor.dto.request.SimulationPosteRequest;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final EmployeeRepository employeeRepository;
    private final ScoreRisqueRepository scoreRisqueRepository;
    private final ObjectMapper objectMapper;

    private static final String ML_URL = "http://localhost:8000/api/ml/predict";

    // ─────────────────────────────────────
    // US12 — Simuler impact augmentation salaire
    // ─────────────────────────────────────
    public SimulationResponse simulerAugmentationSalaire(SimulationSalaireRequest request) {

        // 1. Récupérer l'employé
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employé non trouvé : " + request.getEmployeeId()));

        // 2. Récupérer le score actuel
        ScoreRisque scoreActuel = scoreRisqueRepository
                .findTopByEmployeeIdOrderByDateCalculDesc(employee.getId())
                .orElseThrow(() -> new RuntimeException("Aucun score trouvé pour cet employé"));

        Double probabiliteActuelle = scoreActuel.getProbabilite();

        // 3. Calculer le nouveau salaire simulé
        Double ancienSalaire = employee.getMonthlyIncome().doubleValue();
        Double nouveauSalaire = ancienSalaire * (1 + request.getPourcentageAugmentation() / 100);

        // 4. Construire le payload ML avec le nouveau salaire
        Map<String, Object> payload = buildMLPayload(employee);
        payload.put("MonthlyIncome", nouveauSalaire.intValue());

        // 5. Appeler FastAPI ML
        Double probabiliteSimulee = callMLService(payload);

        // 6. Calculer l'impact
        Double impact = probabiliteActuelle - probabiliteSimulee;

        // 7. Construire la réponse
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
                .recommandation(genererRecommandation(impact, request.getPourcentageAugmentation()))
                .actionRecommandee(impact > 0.05)
                .build();
    }

    // ─────────────────────────────────────
    // Construire le payload ML depuis Employee
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

            // FastAPI retourne "probability" (pas "probabilite")
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
    // Générer une recommandation
    // ─────────────────────────────────────
    private String genererRecommandation(Double impact, Double pourcentage) {
        if (impact > 0.20) {
            return String.format(
                    "✅ Très efficace ! Une augmentation de %.0f%% réduit significativement le risque de départ.",
                    pourcentage);
        } else if (impact > 0.05) {
            return String.format(
                    "👍 Efficace. Une augmentation de %.0f%% améliore la rétention de cet employé.",
                    pourcentage);
        } else if (impact > 0) {
            return String.format(
                    "⚠️ Impact limité. Une augmentation de %.0f%% a peu d'effet sur ce profil.",
                    pourcentage);
        } else {
            return "❌ Inefficace. L'augmentation salariale n'est pas le levier adapté pour cet employé.";
        }
    }



    // ─────────────────────────────────────
// US13 — Simuler impact changement de poste
// ─────────────────────────────────────
    public SimulationResponse simulerChangementPoste(SimulationPosteRequest request) {

        // 1. Récupérer l'employé
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employé non trouvé : " + request.getEmployeeId()));

        // 2. Récupérer le score actuel
        ScoreRisque scoreActuel = scoreRisqueRepository
                .findTopByEmployeeIdOrderByDateCalculDesc(employee.getId())
                .orElseThrow(() -> new RuntimeException("Aucun score trouvé pour cet employé"));

        Double probabiliteActuelle = scoreActuel.getProbabilite();

        // 3. Construire le payload avec le nouveau poste
        Map<String, Object> payload = buildMLPayload(employee);
        payload.put("JobRole",    request.getNouveauPoste().name());
        payload.put("Department", request.getNouveauDepartement().name());
        if (request.getNouveauJobLevel() != null) {
            payload.put("JobLevel", request.getNouveauJobLevel());
        }

        // 4. Appeler FastAPI ML
        Double probabiliteSimulee = callMLService(payload);

        // 5. Calculer l'impact
        Double impact = probabiliteActuelle - probabiliteSimulee;

        // 6. Construire la réponse
        String scenario = String.format(
                "%s → %s (%s)",
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
// Recommandation changement de poste
// ─────────────────────────────────────
    private String genererRecommandationPoste(Double impact, String nouveauPoste) {
        if (impact > 0.20) {
            return String.format(
                    "✅ Très efficace ! Le transfert vers le poste %s réduit significativement le risque.",
                    nouveauPoste);
        } else if (impact > 0.05) {
            return String.format(
                    "👍 Efficace. Le changement vers %s améliore la rétention.",
                    nouveauPoste);
        } else if (impact > 0) {
            return String.format(
                    "⚠️ Impact limité. Le changement vers %s a peu d'effet sur ce profil.",
                    nouveauPoste);
        } else {
            return "❌ Inefficace. Le changement de poste n'est pas le levier adapté pour cet employé.";
        }
    }





    // ─────────────────────────────────────
// US14 — Simuler impact formation
// ─────────────────────────────────────
    public SimulationResponse simulerFormation(SimulationFormationRequest request) {

        // 1. Récupérer l'employé
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employé non trouvé : " + request.getEmployeeId()));

        // 2. Récupérer le score actuel
        ScoreRisque scoreActuel = scoreRisqueRepository
                .findTopByEmployeeIdOrderByDateCalculDesc(employee.getId())
                .orElseThrow(() -> new RuntimeException("Aucun score trouvé pour cet employé"));

        Double probabiliteActuelle = scoreActuel.getProbabilite();

        // 3. Construire le payload avec nouvelles formations
        Integer anciennesFormations = employee.getTrainingTimesLastYear();
        Integer nouvellesFormations = anciennesFormations + request.getNombreFormations();

        Map<String, Object> payload = buildMLPayload(employee);
        payload.put("TrainingTimesLastYear", nouvellesFormations);

        // 4. Appeler FastAPI ML
        Double probabiliteSimulee = callMLService(payload);

        // 5. Calculer l'impact
        Double impact = probabiliteActuelle - probabiliteSimulee;

        // 6. Construire la réponse
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
                .recommandation(genererRecommandationFormation(impact, request.getNombreFormations()))
                .actionRecommandee(impact > 0.05)
                .build();
    }

    // ─────────────────────────────────────
// Recommandation formation
// ─────────────────────────────────────
    private String genererRecommandationFormation(Double impact, Integer nombreFormations) {
        if (impact > 0.20) {
            return String.format(
                    "✅ Très efficace ! %d formation(s) supplémentaire(s) réduit significativement le risque.",
                    nombreFormations);
        } else if (impact > 0.05) {
            return String.format(
                    "👍 Efficace. %d formation(s) supplémentaire(s) améliore la rétention.",
                    nombreFormations);
        } else if (impact > 0) {
            return String.format(
                    "⚠️ Impact limité. %d formation(s) supplémentaire(s) a peu d'effet sur ce profil.",
                    nombreFormations);
        } else {
            return "❌ Inefficace. La formation n'est pas le levier adapté pour cet employé.";
        }
    }




    // ─────────────────────────────────────
// US15 — Comparer tous les scénarios
// ─────────────────────────────────────
    public ComparaisonSimulationResponse comparerScenarios(Long employeeId) {

        // 1. Récupérer l'employé
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé : " + employeeId));

        // 2. Récupérer le score actuel
        ScoreRisque scoreActuel = scoreRisqueRepository
                .findTopByEmployeeIdOrderByDateCalculDesc(employeeId)
                .orElseThrow(() -> new RuntimeException("Aucun score trouvé"));

        Double probabiliteActuelle = scoreActuel.getProbabilite();

        // 3. Simulation salaire +20%
        SimulationSalaireRequest salaireReq = new SimulationSalaireRequest();
        salaireReq.setEmployeeId(employeeId);
        salaireReq.setPourcentageAugmentation(20.0);
        SimulationResponse simSalaire = simulerAugmentationSalaire(salaireReq);

        // 4. Simulation formation +3
        SimulationFormationRequest formationReq = new SimulationFormationRequest();
        formationReq.setEmployeeId(employeeId);
        formationReq.setNombreFormations(3);
        SimulationResponse simFormation = simulerFormation(formationReq);

        // 5. Simulation poste — meilleur poste disponible
        SimulationPosteRequest posteReq = new SimulationPosteRequest();
        posteReq.setEmployeeId(employeeId);
        posteReq.setNouveauPoste(employee.getJobRole());
        posteReq.setNouveauDepartement(employee.getDepartment());
        posteReq.setNouveauJobLevel(employee.getJobLevel() + 1);
        SimulationResponse simPoste = simulerChangementPoste(posteReq);

        // 6. Trouver la meilleure simulation
        SimulationResponse meilleure = simSalaire;
        if (simFormation.getImpactPourcentage() > meilleure.getImpactPourcentage()) {
            meilleure = simFormation;
        }
        if (simPoste.getImpactPourcentage() > meilleure.getImpactPourcentage()) {
            meilleure = simPoste;
        }

        // 7. Construire la réponse
        return ComparaisonSimulationResponse.builder()
                .employeeId(employee.getId())
                .employeeNom(employee.getFirstName() + " " + employee.getLastName())
                .employeeMatricule(employee.getMatricule())
                .probabiliteActuelle(probabiliteActuelle)
                .niveauRisqueActuel(calculerNiveau(probabiliteActuelle).name())
                .simulationSalaire(simSalaire)
                .simulationPoste(simPoste)
                .simulationFormation(simFormation)
                .meilleureAction(meilleure.getRecommandation())
                .typeSimulationRecommande(meilleure.getTypeSimulation())
                .meilleurImpact(meilleure.getImpactPourcentage())
                .build();
    }
}