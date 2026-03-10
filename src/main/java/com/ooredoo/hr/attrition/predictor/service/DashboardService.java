package com.ooredoo.hr.attrition.predictor.service;

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

    // ─────────────────────────────────────
    // US16 — Statistiques globales dashboard
    // ─────────────────────────────────────
    public DashboardStatsResponse getStats() {

        // 1. Statistiques employés
        List<Employee> tousEmployes = employeeRepository.findAll();
        List<Employee> actifs = employeeRepository.findByActiveTrue();
        Long totalEmployes = (long) tousEmployes.size();
        Long totalActifs   = (long) actifs.size();

        // 2. Statistiques alertes
        Long totalAlertes   = (long) alerteRepository.findAll().size();
        Long alertesNonLues = (long) alerteRepository
                .findByStatut(EStatutAlerte.NON_LUE).size();

        // 3. Dernier score de chaque employé actif
        List<ScoreRisque> derniersScores = actifs.stream()
                .map(e -> scoreRisqueRepository
                        .findTopByEmployeeIdOrderByDateCalculDesc(e.getId())
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Long totalPredictions = (long) scoreRisqueRepository.findAll().size();

        // 4. Répartition par niveau de risque
        Long risqueEleve  = derniersScores.stream()
                .filter(s -> s.getNiveauRisque() == ENiveauRisque.ÉLEVÉ).count();
        Long risqueMoyen  = derniersScores.stream()
                .filter(s -> s.getNiveauRisque() == ENiveauRisque.MOYEN).count();
        Long risqueFaible = derniersScores.stream()
                .filter(s -> s.getNiveauRisque() == ENiveauRisque.FAIBLE).count();

        Double tauxRisqueEleve = derniersScores.isEmpty() ? 0.0 :
                (risqueEleve * 100.0) / derniersScores.size();

        // 5. Répartition par département
        Map<String, Long> repartitionDepartement = actifs.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getDepartment().name(),
                        Collectors.counting()));

        // 6. Top 5 employés les plus à risque
        List<DashboardStatsResponse.EmployeeRisqueDTO> top5 = derniersScores.stream()
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

        // 7. Construire la réponse
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
                .top5Risque(top5)
                .build();
    }

    // ─────────────────────────────────────
// US19 — Stats filtrées
// ─────────────────────────────────────
    public DashboardStatsResponse getStatsFiltered(String departement, String niveauRisque) {

        // 1. Employés actifs filtrés par département
        List<Employee> actifs = employeeRepository.findByActiveTrue();

        if (departement != null && !departement.isEmpty()) {
            try {
                EDepartment dept = EDepartment.valueOf(departement);
                actifs = actifs.stream()
                        .filter(e -> e.getDepartment() == dept)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }

        // 2. Derniers scores
        List<ScoreRisque> derniersScores = actifs.stream()
                .map(e -> scoreRisqueRepository
                        .findTopByEmployeeIdOrderByDateCalculDesc(e.getId())
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 3. Filtre par niveau de risque
        if (niveauRisque != null && !niveauRisque.isEmpty()) {
            try {
                ENiveauRisque niveau = ENiveauRisque.valueOf(niveauRisque);
                derniersScores = derniersScores.stream()
                        .filter(s -> s.getNiveauRisque() == niveau)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }

        // 4. Stats
        final List<ScoreRisque> scoresFiltres = derniersScores;
        Long risqueEleve  = scoresFiltres.stream()
                .filter(s -> s.getNiveauRisque() == ENiveauRisque.ÉLEVÉ).count();
        Long risqueMoyen  = scoresFiltres.stream()
                .filter(s -> s.getNiveauRisque() == ENiveauRisque.MOYEN).count();
        Long risqueFaible = scoresFiltres.stream()
                .filter(s -> s.getNiveauRisque() == ENiveauRisque.FAIBLE).count();

        Double tauxRisqueEleve = scoresFiltres.isEmpty() ? 0.0 :
                (risqueEleve * 100.0) / scoresFiltres.size();

        // 5. Top 5
        List<DashboardStatsResponse.EmployeeRisqueDTO> top5 = scoresFiltres.stream()
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

        // 6. Répartition département
        Map<String, Long> repartitionDepartement = actifs.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getDepartment().name(),
                        Collectors.counting()));

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
                .top5Risque(top5)
                .build();
    }
}