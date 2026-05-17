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

    private static final Map<String, String> FEATURE_LABELS = Map.ofEntries(
            Map.entry("JobSatisfaction",         "Satisfaction"),
            Map.entry("tenure_category",         "Ancienneté"),
            Map.entry("overtime_x_joblevel",     "Surcharge"),
            Map.entry("WorkLifeBalance",         "Équilibre vie"),
            Map.entry("EnvironmentSatisfaction", "Ambiance travail"),
            Map.entry("JobInvolvement",          "Engagement"),
            Map.entry("YearsAtCompany",          "Années entreprise"),
            Map.entry("Age",                     "Âge"),
            Map.entry("tenure_satisfaction",     "Ancienneté/Satisfaction"),
            Map.entry("NumCompaniesWorked",      "Mobilité"),
            Map.entry("MonthlyIncome",           "Salaire"),
            Map.entry("StockOptionLevel",        "Actions"),
            Map.entry("YearsSinceLastPromotion", "Dernière promotion"),
            Map.entry("YearsWithCurrManager",    "Stabilité manager"),
            Map.entry("DistanceFromHome",        "Distance domicile"),
            Map.entry("TotalWorkingYears",       "Expérience"),
            Map.entry("job_hopping_score",       "Fidélité"),
            Map.entry("BusinessTravel",          "Déplacements"),
            Map.entry("satisfaction_score",      "Score satisfaction"),
            Map.entry("work_life_balance_score", "Score équilibre"),
            Map.entry("promotion_rate",          "Promotion"),
            Map.entry("DailyRate",               "Taux journalier")
    );

    // ─────────────────────────────────────
    // US16 — Statistiques globales dashboard
    // ─────────────────────────────────────
    public DashboardStatsResponse getStats() {

        List<Employee> tousEmployes = employeeRepository.findAll();
        List<Employee> actifs       = employeeRepository.findByActiveTrue();
        Long totalEmployes = (long) tousEmployes.size();
        Long totalActifs   = (long) actifs.size();

        Long totalAlertes   = (long) alerteRepository.findAll().size();
        Long alertesNonLues = (long) alerteRepository.findByStatut(EStatutAlerte.NON_LUE).size();

        List<ScoreRisque> derniersScores = actifs.stream()
                .map(e -> scoreRisqueRepository
                        .findTopByEmployeeIdOrderByDateCalculDesc(e.getId())
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Long totalPredictions = (long) scoreRisqueRepository.findAll().size();

        Long risqueEleve  = derniersScores.stream().filter(s -> s.getNiveauRisque() == ENiveauRisque.ÉLEVÉ).count();
        Long risqueMoyen  = derniersScores.stream().filter(s -> s.getNiveauRisque() == ENiveauRisque.MOYEN).count();
        Long risqueFaible = derniersScores.stream().filter(s -> s.getNiveauRisque() == ENiveauRisque.FAIBLE).count();

        Double tauxRisqueEleve = derniersScores.isEmpty() ? 0.0 :
                (risqueEleve * 100.0) / derniersScores.size();

        // ── NOUVEAU : Risque global de départ ──
        Double risqueGlobalDepart = derniersScores.isEmpty() ? 0.0 :
                Math.round(
                        derniersScores.stream()
                        .mapToDouble(ScoreRisque::getProbabilite)
                        .average()
                        .orElse(0.0) * 100.0 * 10.0
                ) / 10.0;

        Map<String, Long> repartitionDepartement = actifs.stream()
                .collect(Collectors.groupingBy(e -> e.getDepartment().name(), Collectors.counting()));

        List<DashboardStatsResponse.EmployeeRisqueDTO> top5 = buildTop5(derniersScores);

        Map<String, Map<String, Long>> repartitionRisqueParDept =
                buildRepartitionRisqueParDepartement(derniersScores);

        Map<String, Double> tauxTurnoverParDept =
                buildTauxTurnoverParDepartement(derniersScores, actifs);

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
                .risqueGlobalDepart(risqueGlobalDepart)          // ← AJOUTÉ
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

        Long risqueEleve  = scoresFiltres.stream().filter(s -> s.getNiveauRisque() == ENiveauRisque.ÉLEVÉ).count();
        Long risqueMoyen  = scoresFiltres.stream().filter(s -> s.getNiveauRisque() == ENiveauRisque.MOYEN).count();
        Long risqueFaible = scoresFiltres.stream().filter(s -> s.getNiveauRisque() == ENiveauRisque.FAIBLE).count();

        Double tauxRisqueEleve = scoresFiltres.isEmpty() ? 0.0 :
                (risqueEleve * 100.0) / scoresFiltres.size();

        // ── NOUVEAU : Risque global de départ ──
        Double risqueGlobalDepart = scoresFiltres.isEmpty() ? 0.0 :
                Math.round(
                        scoresFiltres.stream()
                        .mapToDouble(ScoreRisque::getProbabilite)
                        .average()
                        .orElse(0.0) * 100.0 * 10.0
                ) / 10.0;

        Map<String, Long> repartitionDepartement = actifs.stream()
                .collect(Collectors.groupingBy(e -> e.getDepartment().name(), Collectors.counting()));

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
                .risqueGlobalDepart(risqueGlobalDepart)          // ← AJOUTÉ
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

    private List<DashboardStatsResponse.EmployeeRisqueDTO> buildTop5(List<ScoreRisque> scores) {
        return scores.stream()
                .sorted(Comparator.comparingDouble(ScoreRisque::getProbabilite).reversed())
                .limit(5)
                .map(s -> DashboardStatsResponse.EmployeeRisqueDTO.builder()
                        .id(s.getEmployee().getId())
                        .nom(s.getEmployee().getFirstName() + " " + s.getEmployee().getLastName())
                        .matricule(s.getEmployee().getMatricule())
                        .departement(s.getEmployee().getDepartment().name())
                        .probabilite(s.getProbabilite())
                        .niveauRisque(s.getNiveauRisque().name())
                        .build())
                .collect(Collectors.toList());
    }

    private Map<String, Map<String, Long>> buildRepartitionRisqueParDepartement(List<ScoreRisque> scores) {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        for (ScoreRisque score : scores) {
            String dept   = score.getEmployee().getDepartment().name();
            String niveau = score.getNiveauRisque().name();
            result.computeIfAbsent(dept, k -> new LinkedHashMap<>());
            result.get(dept).merge(niveau, 1L, Long::sum);
        }
        for (Map<String, Long> niveaux : result.values()) {
            niveaux.putIfAbsent("ÉLEVÉ",  0L);
            niveaux.putIfAbsent("MOYEN",  0L);
            niveaux.putIfAbsent("FAIBLE", 0L);
        }
        return result;
    }

    private Map<String, Double> buildTauxTurnoverParDepartement(
            List<ScoreRisque> scores, List<Employee> actifs) {
        Map<String, Long> totalParDept = actifs.stream()
                .collect(Collectors.groupingBy(e -> e.getDepartment().name(), Collectors.counting()));
        Map<String, Long> aRisqueParDept = scores.stream()
                .filter(s -> s.getNiveauRisque() == ENiveauRisque.ÉLEVÉ
                        || s.getNiveauRisque() == ENiveauRisque.MOYEN)
                .collect(Collectors.groupingBy(
                        s -> s.getEmployee().getDepartment().name(), Collectors.counting()));
        Map<String, Double> taux = new LinkedHashMap<>();
        for (String dept : totalParDept.keySet()) {
            long total   = totalParDept.getOrDefault(dept, 1L);
            long aRisque = aRisqueParDept.getOrDefault(dept, 0L);
            taux.put(dept, Math.round((aRisque * 100.0 / total) * 10.0) / 10.0);
        }
        return taux;
    }

    private List<DashboardStatsResponse.FacteurRisqueGlobalDTO> buildTopFacteurs(List<ScoreRisque> scores) {
        Map<String, long[]> aggregation = new LinkedHashMap<>();
        for (ScoreRisque score : scores) {
            if (score.getFacteursPrincipaux() == null) continue;
            try {
                List<Map<String, Object>> facteurs = objectMapper.readValue(
                        score.getFacteursPrincipaux(), new TypeReference<>() {});
                for (Map<String, Object> facteur : facteurs) {
                    String impact = (String) facteur.get("impact");
                    if (!"AUGMENTE".equals(impact)) continue;
                    String feature   = (String) facteur.get("feature");
                    double shapValue = ((Number) facteur.get("shap_value")).doubleValue();
                    aggregation.computeIfAbsent(feature, k -> new long[]{0, 0});
                    aggregation.get(feature)[0]++;
                    aggregation.get(feature)[1] += (long)(Math.abs(shapValue) * 1000);
                }
            } catch (Exception ignored) {}
        }
        if (aggregation.isEmpty()) return Collections.emptyList();
        long totalCount = aggregation.values().stream().mapToLong(arr -> arr[0]).sum();
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