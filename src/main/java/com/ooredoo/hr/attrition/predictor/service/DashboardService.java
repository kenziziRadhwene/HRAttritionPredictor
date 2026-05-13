package com.ooredoo.hr.attrition.predictor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ooredoo.hr.attrition.predictor.dto.response.DashboardStatsResponse;
import com.ooredoo.hr.attrition.predictor.entity.Employee;
import com.ooredoo.hr.attrition.predictor.entity.ScoreRisque;
import com.ooredoo.hr.attrition.predictor.enums.EDepartment;
import com.ooredoo.hr.attrition.predictor.enums.ENiveauRisque;
import com.ooredoo.hr.attrition.predictor.enums.EStatutAlerte;
import com.ooredoo.hr.attrition.predictor.repository.AlerteRepository;
import com.ooredoo.hr.attrition.predictor.repository.EmployeeRepository;
import com.ooredoo.hr.attrition.predictor.repository.ScoreRisqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final ScoreRisqueRepository scoreRisqueRepository;
    private final AlerteRepository alerteRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Labels lisibles pour les features SHAP
    private static final Map<String, String> FEATURE_LABELS = Map.ofEntries(
            Map.entry("JobSatisfaction",         "Satisfaction au travail"),
            Map.entry("tenure_category",         "Catégorie ancienneté"),
            Map.entry("overtime_x_joblevel",     "Heures sup × Niveau poste"),
            Map.entry("WorkLifeBalance",         "Équilibre vie pro/perso"),
            Map.entry("EnvironmentSatisfaction", "Satisfaction environnement"),
            Map.entry("JobInvolvement",          "Implication au travail"),
            Map.entry("YearsAtCompany",          "Années dans l'entreprise"),
            Map.entry("Age",                     "Âge"),
            Map.entry("tenure_satisfaction",     "Ancienneté × Satisfaction"),
            Map.entry("NumCompaniesWorked",      "Nombre d'entreprises"),
            Map.entry("MonthlyIncome",           "Salaire mensuel"),
            Map.entry("StockOptionLevel",        "Options sur actions"),
            Map.entry("YearsSinceLastPromotion", "Depuis dernière promotion"),
            Map.entry("YearsWithCurrManager",    "Années avec manager actuel"),
            Map.entry("DistanceFromHome",        "Distance domicile"),
            Map.entry("TotalWorkingYears",       "Années d'expérience totale"),
            Map.entry("job_hopping_score",  "Score mobilité entreprises"),
            Map.entry("BusinessTravel",     "Déplacements professionnels"),
            Map.entry("satisfaction_score", "Score satisfaction global"),
            Map.entry("work_life_balance_score", "Score équilibre vie"),
            Map.entry("promotion_rate",     "Taux de promotion"),
            Map.entry("DailyRate",          "Taux journalier")
    );

    // ─────────────────────────────────────
    // US16 — Statistiques globales dashboard
    // ─────────────────────────────────────
    public DashboardStatsResponse getStats() {

        List<Employee> tousEmployes = employeeRepository.findAll();
        List<Employee> actifs = employeeRepository.findByActiveTrue();
        Long totalEmployes = (long) tousEmployes.size();
        Long totalActifs   = (long) actifs.size();

        Long totalAlertes   = (long) alerteRepository.findAll().size();
        Long alertesNonLues = (long) alerteRepository
                .findByStatut(EStatutAlerte.NON_LUE).size();

        // Dernier score de chaque employé actif
        List<ScoreRisque> derniersScores = actifs.stream()
                .map(e -> scoreRisqueRepository
                        .findTopByEmployeeIdOrderByDateCalculDesc(e.getId())
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Long totalPredictions = (long) scoreRisqueRepository.findAll().size();

        // Répartition par niveau de risque
        Long risqueEleve  = derniersScores.stream()
                .filter(s -> s.getNiveauRisque() == ENiveauRisque.ÉLEVÉ).count();
        Long risqueMoyen  = derniersScores.stream()
                .filter(s -> s.getNiveauRisque() == ENiveauRisque.MOYEN).count();
        Long risqueFaible = derniersScores.stream()
                .filter(s -> s.getNiveauRisque() == ENiveauRisque.FAIBLE).count();

        Double tauxRisqueEleve = derniersScores.isEmpty() ? 0.0 :
                (risqueEleve * 100.0) / derniersScores.size();

        // Répartition par département (existant)
        Map<String, Long> repartitionDepartement = actifs.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getDepartment().name(),
                        Collectors.counting()));

        // Top 5
        List<DashboardStatsResponse.EmployeeRisqueDTO> top5 = buildTop5(derniersScores);

        // ── NOUVEAU : Répartition risque par département ──
        Map<String, Map<String, Long>> repartitionRisqueParDept =
                buildRepartitionRisqueParDepartement(derniersScores);

        // ── NOUVEAU : Taux turnover par département ──
        Map<String, Double> tauxTurnoverParDept =
                buildTauxTurnoverParDepartement(derniersScores, actifs);

        // ── NOUVEAU : Top facteurs de risque globaux ──
        List<DashboardStatsResponse.FacteurRisqueGlobalDTO> topFacteurs =
                buildTopFacteurs(derniersScores);

        return DashboardStatsResponse.builder()
                .totalEmployes(totalEmployes)
                .totalActifs(totalActifs)
                .totalAlertes(totalAlertes)
                .alertesNonLues(alertesNonLues)
                .totalPredictions(totalPredictions)
                .risqueEleve(risqueEleve)
                .risqueMoyen(risqueMoyen)
                .risqueFaible(risqueFaible)
                .tauxRisqueEleve(tauxRisqueEleve)
                .repartitionDepartement(repartitionDepartement)
                .repartitionRisqueParDepartement(repartitionRisqueParDept)
                .tauxTurnoverParDepartement(tauxTurnoverParDept)
                .topFacteursRisque(topFacteurs)
                .top5Risque(top5)
                .build();
    }

    // ─────────────────────────────────────
    // US19 — Stats filtrées
    // ─────────────────────────────────────
    public DashboardStatsResponse getStatsFiltered(String departement, String niveauRisque) {

        List<Employee> actifs = employeeRepository.findByActiveTrue();

        if (departement != null && !departement.isEmpty()) {
            try {
                EDepartment dept = EDepartment.valueOf(departement);
                actifs = actifs.stream()
                        .filter(e -> e.getDepartment() == dept)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }

        List<ScoreRisque> derniersScores = actifs.stream()
                .map(e -> scoreRisqueRepository
                        .findTopByEmployeeIdOrderByDateCalculDesc(e.getId())
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (niveauRisque != null && !niveauRisque.isEmpty()) {
            try {
                ENiveauRisque niveau = ENiveauRisque.valueOf(niveauRisque);
                derniersScores = derniersScores.stream()
                        .filter(s -> s.getNiveauRisque() == niveau)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }

        final List<ScoreRisque> scoresFiltres = derniersScores;
        Long risqueEleve  = scoresFiltres.stream()
                .filter(s -> s.getNiveauRisque() == ENiveauRisque.ÉLEVÉ).count();
        Long risqueMoyen  = scoresFiltres.stream()
                .filter(s -> s.getNiveauRisque() == ENiveauRisque.MOYEN).count();
        Long risqueFaible = scoresFiltres.stream()
                .filter(s -> s.getNiveauRisque() == ENiveauRisque.FAIBLE).count();

        Double tauxRisqueEleve = scoresFiltres.isEmpty() ? 0.0 :
                (risqueEleve * 100.0) / scoresFiltres.size();

        Map<String, Long> repartitionDepartement = actifs.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getDepartment().name(),
                        Collectors.counting()));

        Map<String, Map<String, Long>> repartitionRisqueParDept =
                buildRepartitionRisqueParDepartement(scoresFiltres);

        Map<String, Double> tauxTurnoverParDept =
                buildTauxTurnoverParDepartement(scoresFiltres, actifs);

        List<DashboardStatsResponse.FacteurRisqueGlobalDTO> topFacteurs =
                buildTopFacteurs(scoresFiltres);

        return DashboardStatsResponse.builder()
                .totalEmployes((long) employeeRepository.findAll().size())
                .totalActifs((long) actifs.size())
                .totalAlertes((long) alerteRepository.findAll().size())
                .alertesNonLues((long) alerteRepository.findByStatut(EStatutAlerte.NON_LUE).size())
                .totalPredictions((long) scoreRisqueRepository.findAll().size())
                .risqueEleve(risqueEleve)
                .risqueMoyen(risqueMoyen)
                .risqueFaible(risqueFaible)
                .tauxRisqueEleve(tauxRisqueEleve)
                .repartitionDepartement(repartitionDepartement)
                .repartitionRisqueParDepartement(repartitionRisqueParDept)
                .tauxTurnoverParDepartement(tauxTurnoverParDept)
                .topFacteursRisque(topFacteurs)
                .top5Risque(buildTop5(scoresFiltres))
                .build();
    }

    // ─────────────────────────────────────
    // Méthodes privées helper
    // ─────────────────────────────────────

    private List<DashboardStatsResponse.EmployeeRisqueDTO> buildTop5(
            List<ScoreRisque> scores) {
        return scores.stream()
                .sorted(Comparator.comparingDouble(ScoreRisque::getProbabilite).reversed())
                .limit(5)
                .map(s -> DashboardStatsResponse.EmployeeRisqueDTO.builder()
                        .id(s.getEmployee().getId())
                        .nom(s.getEmployee().getFirstName()
                                + " " + s.getEmployee().getLastName())
                        .matricule(s.getEmployee().getMatricule())
                        .departement(s.getEmployee().getDepartment().name())
                        .probabilite(s.getProbabilite())
                        .niveauRisque(s.getNiveauRisque().name())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Répartition ÉLEVÉ / MOYEN / FAIBLE par département
     * { "DIRECTION_TECHNOLOGIQUE": { "ÉLEVÉ": 45, "MOYEN": 30, "FAIBLE": 10 } }
     */
    private Map<String, Map<String, Long>> buildRepartitionRisqueParDepartement(
            List<ScoreRisque> scores) {

        Map<String, Map<String, Long>> result = new LinkedHashMap<>();

        for (ScoreRisque score : scores) {
            String dept   = score.getEmployee().getDepartment().name();
            String niveau = score.getNiveauRisque().name();

            result.computeIfAbsent(dept, k -> new LinkedHashMap<>());
            result.get(dept).merge(niveau, 1L, Long::sum);
        }

        // S'assurer que chaque département a les 3 niveaux (même à 0)
        for (Map<String, Long> niveaux : result.values()) {
            niveaux.putIfAbsent("ÉLEVÉ",  0L);
            niveaux.putIfAbsent("MOYEN",  0L);
            niveaux.putIfAbsent("FAIBLE", 0L);
        }

        return result;
    }

    /**
     * Taux de turnover prédit par département
     * = (nb employés ÉLEVÉ + MOYEN) / total département × 100
     */
    private Map<String, Double> buildTauxTurnoverParDepartement(
            List<ScoreRisque> scores, List<Employee> actifs) {

        // Total employés par département
        Map<String, Long> totalParDept = actifs.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getDepartment().name(),
                        Collectors.counting()));

        // Employés à risque (ÉLEVÉ ou MOYEN) par département
        Map<String, Long> aRisqueParDept = scores.stream()
                .filter(s -> s.getNiveauRisque() == ENiveauRisque.ÉLEVÉ
                        || s.getNiveauRisque() == ENiveauRisque.MOYEN)
                .collect(Collectors.groupingBy(
                        s -> s.getEmployee().getDepartment().name(),
                        Collectors.counting()));

        Map<String, Double> taux = new LinkedHashMap<>();
        for (String dept : totalParDept.keySet()) {
            long total    = totalParDept.getOrDefault(dept, 1L);
            long aRisque  = aRisqueParDept.getOrDefault(dept, 0L);
            taux.put(dept, Math.round((aRisque * 100.0 / total) * 10.0) / 10.0);
        }

        return taux;
    }

    /**
     * Top facteurs de risque globaux depuis les SHAP de tous les derniers scores
     * Agrège les facteurs AUGMENTE et calcule leur fréquence + SHAP moyen
     */
    private List<DashboardStatsResponse.FacteurRisqueGlobalDTO> buildTopFacteurs(
            List<ScoreRisque> scores) {

        // Map : feature → { count, sumShap }
        Map<String, long[]> aggregation = new LinkedHashMap<>();

        for (ScoreRisque score : scores) {
            if (score.getFacteursPrincipaux() == null) continue;
            try {
                List<Map<String, Object>> facteurs = objectMapper.readValue(
                        score.getFacteursPrincipaux(),
                        new TypeReference<>() {});

                for (Map<String, Object> facteur : facteurs) {
                    String impact = (String) facteur.get("impact");
                    if (!"AUGMENTE".equals(impact)) continue;

                    String feature   = (String) facteur.get("feature");
                    double shapValue = ((Number) facteur.get("shap_value")).doubleValue();

                    aggregation.computeIfAbsent(feature, k -> new long[]{0, 0});
                    aggregation.get(feature)[0]++;                               // count
                    aggregation.get(feature)[1] += (long)(Math.abs(shapValue) * 1000); // sum * 1000
                }
            } catch (Exception ignored) {}
        }

        if (aggregation.isEmpty()) return Collections.emptyList();

        // Calculer le total pour les pourcentages
        long totalCount = aggregation.values().stream()
                .mapToLong(arr -> arr[0])
                .sum();

        // Construire la liste triée par count décroissant, limite 8
        return aggregation.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(8)
                .map(entry -> {
                    String feature = entry.getKey();
                    long count     = entry.getValue()[0];
                    double avgShap = (entry.getValue()[1] / 1000.0) / count;
                    double pct     = Math.round((count * 100.0 / totalCount) * 10.0) / 10.0;

                    return DashboardStatsResponse.FacteurRisqueGlobalDTO.builder()
                            .feature(feature)
                            .featureLabel(FEATURE_LABELS.getOrDefault(feature, feature))
                            .count(count)
                            .avgShapValue(Math.round(avgShap * 100.0) / 100.0)
                            .pourcentage(pct)
                            .build();
                })
                .collect(Collectors.toList());
    }
}