package com.ooredoo.hr.attrition.predictor.service;

import com.ooredoo.hr.attrition.predictor.dto.request.EmployeeRequest;
import com.ooredoo.hr.attrition.predictor.dto.response.EmployeeResponse;
import com.ooredoo.hr.attrition.predictor.entity.Employee;
import com.ooredoo.hr.attrition.predictor.entity.ScoreRisque;
import com.ooredoo.hr.attrition.predictor.enums.EDepartment;
import com.ooredoo.hr.attrition.predictor.enums.EAuditAction;
import com.ooredoo.hr.attrition.predictor.repository.EmployeeRepository;
import com.ooredoo.hr.attrition.predictor.repository.ScoreRisqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final ScoreRisqueRepository scoreRisqueRepository;
    private final AuditLogService auditLogService;

    public EmployeeResponse createEmployee(EmployeeRequest request) {

        if (employeeRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Email déjà utilisé : " + request.getEmail());

        if (employeeRepository.existsByMatricule(request.getMatricule()))
            throw new RuntimeException("Matricule déjà utilisé : " + request.getMatricule());

        Employee employee = Employee.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .age(request.getAge())
                .gender(request.getGender())
                .maritalStatus(request.getMaritalStatus())
                .distanceFromHome(request.getDistanceFromHome())
                .matricule(request.getMatricule())
                .department(request.getDepartment())
                .jobRole(request.getJobRole())
                .jobLevel(request.getJobLevel())
                .businessTravel(request.getBusinessTravel())
                .educationField(request.getEducationField())
                .education(request.getEducation())
                .monthlyIncome(request.getMonthlyIncome())
                .dailyRate(request.getDailyRate())
                .hourlyRate(request.getHourlyRate())
                .monthlyRate(request.getMonthlyRate())
                .percentSalaryHike(request.getPercentSalaryHike())
                .stockOptionLevel(request.getStockOptionLevel())
                .jobSatisfaction(request.getJobSatisfaction())
                .environmentSatisfaction(request.getEnvironmentSatisfaction())
                .relationshipSatisfaction(request.getRelationshipSatisfaction())
                .jobInvolvement(request.getJobInvolvement())
                .workLifeBalance(request.getWorkLifeBalance())
                .performanceRating(request.getPerformanceRating())
                .overTime(request.getOverTime())
                .numCompaniesWorked(request.getNumCompaniesWorked())
                .totalWorkingYears(request.getTotalWorkingYears())
                .yearsAtCompany(request.getYearsAtCompany())
                .yearsInCurrentRole(request.getYearsInCurrentRole())
                .yearsSinceLastPromotion(request.getYearsSinceLastPromotion())
                .yearsWithCurrManager(request.getYearsWithCurrManager())
                .trainingTimesLastYear(request.getTrainingTimesLastYear())
                .active(true)
                .build();

        Employee saved = employeeRepository.save(employee);

        auditLogService.log(
                EAuditAction.EMPLOYEE_CREATE,
                "employees",
                saved.getId(),
                "{\"matricule\": \"" + saved.getMatricule() + "\", \"nom\": \""
                        + saved.getFirstName() + " " + saved.getLastName() + "\"}"
        );

        return toResponse(saved);
    }

    public List<EmployeeResponse> getAllEmployees() {
        List<Employee> actifs = employeeRepository.findByActiveTrue();

        Map<Long, ScoreRisque> derniersScores = scoreRisqueRepository
                .findDerniersScoresEmployesActifs()
                .stream()
                .collect(Collectors.toMap(
                        s -> s.getEmployee().getId(),
                        s -> s
                ));

        return actifs.stream()
                .map(e -> toResponseWithScore(e, derniersScores.get(e.getId())))
                .collect(Collectors.toList());
    }

    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Employé non trouvé avec l'ID : " + id));
        return toResponse(employee);
    }

    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Employé non trouvé avec l'ID : " + id));

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setAge(request.getAge());
        employee.setGender(request.getGender());
        employee.setMaritalStatus(request.getMaritalStatus());
        employee.setDistanceFromHome(request.getDistanceFromHome());
        employee.setDepartment(request.getDepartment());
        employee.setJobRole(request.getJobRole());
        employee.setJobLevel(request.getJobLevel());
        employee.setBusinessTravel(request.getBusinessTravel());
        employee.setEducationField(request.getEducationField());
        employee.setEducation(request.getEducation());
        employee.setMonthlyIncome(request.getMonthlyIncome());
        employee.setDailyRate(request.getDailyRate());
        employee.setHourlyRate(request.getHourlyRate());
        employee.setMonthlyRate(request.getMonthlyRate());
        employee.setPercentSalaryHike(request.getPercentSalaryHike());
        employee.setStockOptionLevel(request.getStockOptionLevel());
        employee.setJobSatisfaction(request.getJobSatisfaction());
        employee.setEnvironmentSatisfaction(request.getEnvironmentSatisfaction());
        employee.setRelationshipSatisfaction(request.getRelationshipSatisfaction());
        employee.setJobInvolvement(request.getJobInvolvement());
        employee.setWorkLifeBalance(request.getWorkLifeBalance());
        employee.setPerformanceRating(request.getPerformanceRating());
        employee.setOverTime(request.getOverTime());
        employee.setNumCompaniesWorked(request.getNumCompaniesWorked());
        employee.setTotalWorkingYears(request.getTotalWorkingYears());
        employee.setYearsAtCompany(request.getYearsAtCompany());
        employee.setYearsInCurrentRole(request.getYearsInCurrentRole());
        employee.setYearsSinceLastPromotion(request.getYearsSinceLastPromotion());
        employee.setYearsWithCurrManager(request.getYearsWithCurrManager());
        employee.setTrainingTimesLastYear(request.getTrainingTimesLastYear());

        Employee updated = employeeRepository.save(employee);

        auditLogService.log(
                EAuditAction.EMPLOYEE_UPDATE,
                "employees",
                updated.getId(),
                "{\"matricule\": \"" + updated.getMatricule() + "\"}"
        );

        return toResponse(updated);
    }

    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Employé non trouvé avec l'ID : " + id));
        employee.setActive(false);
        employeeRepository.save(employee);

        auditLogService.log(
                EAuditAction.EMPLOYEE_DELETE,
                "employees",
                id,
                "{\"matricule\": \"" + employee.getMatricule() + "\"}"
        );
    }

    public List<EmployeeResponse> getEmployeesByDepartment(String department) {
        try {
            EDepartment dept = EDepartment.valueOf(department.toUpperCase());
            List<Employee> employes = employeeRepository
                    .findByActiveTrueAndDepartment(dept);

            Map<Long, ScoreRisque> derniersScores = scoreRisqueRepository
                    .findDerniersScoresEmployesActifs()
                    .stream()
                    .collect(Collectors.toMap(
                            s -> s.getEmployee().getId(),
                            s -> s
                    ));

            return employes.stream()
                    .map(e -> toResponseWithScore(e, derniersScores.get(e.getId())))
                    .collect(Collectors.toList());

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Département invalide : " + department);
        }
    }

    public EmployeeResponse toResponseWithScore(Employee employee,
                                                ScoreRisque dernierScore) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .age(employee.getAge())
                .gender(employee.getGender())
                .maritalStatus(employee.getMaritalStatus())
                .distanceFromHome(employee.getDistanceFromHome())
                .matricule(employee.getMatricule())
                .department(employee.getDepartment())
                .jobRole(employee.getJobRole())
                .jobLevel(employee.getJobLevel())
                .businessTravel(employee.getBusinessTravel())
                .monthlyIncome(employee.getMonthlyIncome())
                .stockOptionLevel(employee.getStockOptionLevel())
                .jobSatisfaction(employee.getJobSatisfaction())
                .environmentSatisfaction(employee.getEnvironmentSatisfaction())
                .workLifeBalance(employee.getWorkLifeBalance())
                .overTime(employee.getOverTime())
                .yearsAtCompany(employee.getYearsAtCompany())
                .yearsSinceLastPromotion(employee.getYearsSinceLastPromotion())
                .active(employee.getActive())
                .educationField(employee.getEducationField())
                .education(employee.getEducation())
                .dailyRate(employee.getDailyRate())
                .hourlyRate(employee.getHourlyRate())
                .monthlyRate(employee.getMonthlyRate())
                .percentSalaryHike(employee.getPercentSalaryHike())
                .relationshipSatisfaction(employee.getRelationshipSatisfaction())
                .jobInvolvement(employee.getJobInvolvement())
                .performanceRating(employee.getPerformanceRating())
                .numCompaniesWorked(employee.getNumCompaniesWorked())
                .totalWorkingYears(employee.getTotalWorkingYears())
                .yearsInCurrentRole(employee.getYearsInCurrentRole())
                .yearsWithCurrManager(employee.getYearsWithCurrManager())
                .trainingTimesLastYear(employee.getTrainingTimesLastYear())
                .createdAt(employee.getCreatedAt())
                .derniereProbabilite(dernierScore != null
                        ? dernierScore.getProbabilite() : null)
                .dernierNiveauRisque(dernierScore != null
                        ? dernierScore.getNiveauRisque().name() : null)
                .derniereDateCalcul(dernierScore != null
                        ? dernierScore.getDateCalcul() : null)
                .build();
    }

    public EmployeeResponse toResponse(Employee employee) {
        ScoreRisque dernierScore = scoreRisqueRepository
                .findTopByEmployeeIdOrderByDateCalculDesc(employee.getId())
                .orElse(null);
        return toResponseWithScore(employee, dernierScore);
    }
}