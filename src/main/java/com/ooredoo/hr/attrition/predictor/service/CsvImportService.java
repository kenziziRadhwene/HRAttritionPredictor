package com.ooredoo.hr.attrition.predictor.service;

import com.ooredoo.hr.attrition.predictor.dto.response.ImportResultResponse;
import com.ooredoo.hr.attrition.predictor.entity.Employee;
import com.ooredoo.hr.attrition.predictor.entity.ImportSession;
import com.ooredoo.hr.attrition.predictor.entity.User;
import com.ooredoo.hr.attrition.predictor.enums.*;
import com.ooredoo.hr.attrition.predictor.repository.EmployeeRepository;
import com.ooredoo.hr.attrition.predictor.repository.ImportSessionRepository;
import com.ooredoo.hr.attrition.predictor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final EmployeeRepository employeeRepository;
    private final ImportSessionRepository importSessionRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    private static final List<String> REQUIRED_COLUMNS = List.of(
            "EmployeeNumber", "Age", "Gender", "Department",
            "JobRole", "MonthlyIncome", "OverTime"
    );
    private static final double ERROR_THRESHOLD = 0.20;

    @Transactional
    public ImportResultResponse importEmployees(MultipartFile file) {

        List<String> erreurs = new ArrayList<>();
        User currentUser = getCurrentUser();

        String fileError = validateFile(file);
        if (fileError != null) {
            sauvegarderSession(currentUser, file, 0, 0, 0, 0, 1,
                    true, EStatutImport.ERREUR_FICHIER, List.of(fileError));
            return ImportResultResponse.builder()
                    .totalLignes(0).crees(0).misAJour(0)
                    .ignores(0).erreurs(1)
                    .detailsErreurs(List.of(fileError))
                    .annule(false)
                    .build();
        }

        try (Reader reader = new InputStreamReader(
                file.getInputStream(), StandardCharsets.UTF_8)) {

            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim()
                    .parse(reader);

            String structureError = validateStructure(
                    parser.getHeaderMap().keySet());
            if (structureError != null) {
                sauvegarderSession(currentUser, file, 0, 0, 0, 0, 1,
                        true, EStatutImport.ERREUR_FICHIER,
                        List.of(structureError));
                return ImportResultResponse.builder()
                        .totalLignes(0).crees(0).misAJour(0)
                        .ignores(0).erreurs(1)
                        .detailsErreurs(List.of(structureError))
                        .annule(false)
                        .build();
            }

            List<Employee> toSave = new ArrayList<>();
            int totalLignes = 0;
            int crees = 0, misAJour = 0;

            for (CSVRecord record : parser) {
                totalLignes++;
                try {
                    Employee employee = parseLigne(record, totalLignes);
                    toSave.add(employee);
                    String matricule = "OO-" + getField(record, "EmployeeNumber",
                            String.valueOf(totalLignes));
                    if (employeeRepository.findByMatricule(matricule).isEmpty()) {
                        crees++;
                    } else {
                        misAJour++;
                    }
                } catch (Exception e) {
                    erreurs.add("Ligne " + totalLignes + " : " + e.getMessage());
                    log.warn("Erreur ligne {} : {}", totalLignes, e.getMessage());
                }
            }

            if (totalLignes > 0) {
                double tauxErreur = (double) erreurs.size() / totalLignes;
                if (tauxErreur > ERROR_THRESHOLD) {
                    erreurs.add(0, String.format(
                            "❌ Import annulé : taux d'erreurs trop élevé (%.0f%% > 20%%)" +
                                    " — Aucune donnée insérée.", tauxErreur * 100));
                    sauvegarderSession(currentUser, file, totalLignes, 0, 0, 0,
                            erreurs.size(), true, EStatutImport.ANNULE, erreurs);
                    return ImportResultResponse.builder()
                            .totalLignes(totalLignes)
                            .crees(0).misAJour(0).ignores(0)
                            .erreurs(erreurs.size())
                            .detailsErreurs(erreurs)
                            .annule(true)
                            .build();
                }
            }

            employeeRepository.saveAll(toSave);
            int ignores = Math.max(
                    totalLignes - crees - misAJour - erreurs.size(), 0);

            ImportSession session = sauvegarderSession(currentUser, file,
                    totalLignes, crees, misAJour, ignores,
                    erreurs.size(), false, EStatutImport.SUCCES, erreurs);

            auditLogService.log(
                    EAuditAction.EMPLOYEE_IMPORT,
                    "import_sessions",
                    session.getId(),
                    String.format(
                            "{\"fichier\": \"%s\", \"crees\": %d, \"misAJour\": %d}",
                            file.getOriginalFilename(), crees, misAJour)
            );

            return ImportResultResponse.builder()
                    .totalLignes(totalLignes)
                    .crees(crees)
                    .misAJour(misAJour)
                    .ignores(ignores)
                    .erreurs(erreurs.size())
                    .detailsErreurs(erreurs)
                    .annule(false)
                    .build();

        } catch (Exception e) {
            log.error("Erreur import CSV : {}", e.getMessage());
            sauvegarderSession(currentUser, file, 0, 0, 0, 0, 1,
                    true, EStatutImport.ERREUR_FICHIER,
                    List.of("Erreur lecture fichier : " + e.getMessage()));
            return ImportResultResponse.builder()
                    .totalLignes(0).crees(0).misAJour(0)
                    .ignores(0).erreurs(1)
                    .detailsErreurs(List.of("Erreur lecture fichier : "
                            + e.getMessage()))
                    .annule(false)
                    .build();
        }
    }

    private ImportSession sauvegarderSession(User user, MultipartFile file,
                                             int totalLignes, int crees, int misAJour, int ignores,
                                             int erreurs, boolean annule, EStatutImport statut,
                                             List<String> detailsErreurs) {
        try {
            String details = detailsErreurs != null && !detailsErreurs.isEmpty()
                    ? String.join("\n", detailsErreurs) : null;

            ImportSession session = ImportSession.builder()
                    .uploadedBy(user)
                    .fichierNom(file.getOriginalFilename())
                    .fichierTaille(file.getSize())
                    .totalLignes(totalLignes)
                    .nbCrees(crees)
                    .nbMisAJour(misAJour)
                    .nbIgnores(ignores)
                    .nbErreurs(erreurs)
                    .annule(annule)
                    .statut(statut)
                    .detailsErreurs(details)
                    .build();

            return importSessionRepository.save(session);
        } catch (Exception e) {
            log.error("❌ Erreur sauvegarde ImportSession: {}", e.getMessage());
            return new ImportSession();
        }
    }

    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || auth.getPrincipal().equals("anonymousUser")) {
                return null;
            }
            return userRepository.findByEmail(auth.getName()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return "Le fichier est vide.";
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv"))
            return "Format invalide. Seuls les fichiers .csv sont acceptés.";
        if (file.getSize() > 10 * 1024 * 1024)
            return "Fichier trop volumineux. Taille maximale : 10 MB.";
        return null;
    }

    private String validateStructure(Set<String> headers) {
        List<String> missing = new ArrayList<>();
        for (String col : REQUIRED_COLUMNS) {
            boolean found = headers.stream()
                    .anyMatch(h -> h.equalsIgnoreCase(col));
            if (!found) missing.add(col);
        }
        if (!missing.isEmpty())
            return "Colonnes obligatoires manquantes : "
                    + String.join(", ", missing);
        return null;
    }

    private Employee parseLigne(CSVRecord record, int numLigne) throws Exception {
        String employeeNumber = getField(record, "EmployeeNumber",
                String.valueOf(numLigne));
        String matricule = "OO-" + employeeNumber;

        int age = parseInt(record, "Age", 35);
        if (age < 18 || age > 70)
            throw new Exception("Age invalide (" + age
                    + "). Doit être entre 18 et 70.");

        int salaire = parseInt(record, "MonthlyIncome", 3000);
        if (salaire < 0)
            throw new Exception("Salaire invalide (" + salaire
                    + "). Doit être positif.");

        Optional<Employee> existing = employeeRepository.findByMatricule(matricule);
        Employee employee = existing.orElse(new Employee());
        boolean isNew = existing.isEmpty();

        employee.setAge(age);
        employee.setGender(parseGender(getField(record, "Gender", "Male")));
        employee.setMaritalStatus(
                parseMaritalStatus(getField(record, "MaritalStatus", "Single")));
        employee.setDistanceFromHome(
                parseIntWithDefault(record, "DistanceFromHome", 5, 0, 100));
        employee.setEducation(
                parseIntWithDefault(record, "Education", 3, 1, 5));
        employee.setEducationField(getField(record, "EducationField", "Other"));
        employee.setMatricule(matricule);
        employee.setDepartment(
                parseDepartment(getField(record, "Department", "Sales")));
        employee.setJobRole(
                parseJobRole(getField(record, "JobRole", "Sales Executive")));
        employee.setJobLevel(
                parseIntWithDefault(record, "JobLevel", 1, 1, 5));
        employee.setBusinessTravel(
                getField(record, "BusinessTravel", "Travel_Rarely"));

        if (isNew) {
            String firstName = getField(record, "FirstName", "");
            String lastName  = getField(record, "LastName", "");
            String email     = getField(record, "Email", "");
            if (firstName.isBlank()) firstName = "Employe";
            if (lastName.isBlank())  lastName  = employeeNumber;
            employee.setFirstName(firstName);
            employee.setLastName(lastName);
            employee.setEmail(email);
        }

        employee.setMonthlyIncome(salaire);
        employee.setDailyRate(
                parseIntWithDefault(record, "DailyRate", 500, 0, 10000));
        employee.setHourlyRate(
                parseIntWithDefault(record, "HourlyRate", 60, 0, 1000));
        employee.setMonthlyRate(
                parseIntWithDefault(record, "MonthlyRate", 15000, 0, 100000));
        employee.setPercentSalaryHike(
                parseIntWithDefault(record, "PercentSalaryHike", 10, 0, 100));
        employee.setStockOptionLevel(
                parseIntWithDefault(record, "StockOptionLevel", 0, 0, 3));
        employee.setJobSatisfaction(
                parseIntWithDefault(record, "JobSatisfaction", 3, 1, 4));
        employee.setEnvironmentSatisfaction(
                parseIntWithDefault(record, "EnvironmentSatisfaction", 3, 1, 4));
        employee.setRelationshipSatisfaction(
                parseIntWithDefault(record, "RelationshipSatisfaction", 3, 1, 4));
        employee.setJobInvolvement(
                parseIntWithDefault(record, "JobInvolvement", 3, 1, 4));
        employee.setWorkLifeBalance(
                parseIntWithDefault(record, "WorkLifeBalance", 3, 1, 4));
        employee.setPerformanceRating(
                parseIntWithDefault(record, "PerformanceRating", 3, 1, 4));
        employee.setOverTime(
                parseBoolean(getField(record, "OverTime", "No")));
        employee.setNumCompaniesWorked(
                parseIntWithDefault(record, "NumCompaniesWorked", 1, 0, 20));
        employee.setTotalWorkingYears(
                parseIntWithDefault(record, "TotalWorkingYears", 5, 0, 50));
        employee.setYearsAtCompany(
                parseIntWithDefault(record, "YearsAtCompany", 3, 0, 40));
        employee.setYearsInCurrentRole(
                parseIntWithDefault(record, "YearsInCurrentRole", 2, 0, 20));
        employee.setYearsSinceLastPromotion(
                parseIntWithDefault(record, "YearsSinceLastPromotion", 1, 0, 20));
        employee.setYearsWithCurrManager(
                parseIntWithDefault(record, "YearsWithCurrManager", 2, 0, 20));
        employee.setTrainingTimesLastYear(
                parseIntWithDefault(record, "TrainingTimesLastYear", 2, 0, 10));

        // ✅ Lecture du statut depuis le CSV (ACTIVE par défaut si absent)
        EStatutEmployee statut = parseStatut(getField(record, "status", "ACTIVE"));
        employee.setStatut(statut);
        employee.setActive(EStatutEmployee.ACTIVE.equals(statut));

        return employee;
    }

    private String getField(CSVRecord record, String field, String defaultVal) {
        try {
            String val = record.get(field);
            return (val == null || val.isBlank()) ? defaultVal : val.trim();
        } catch (Exception e) { return defaultVal; }
    }

    private int parseInt(CSVRecord record, String field, int defaultVal) {
        try {
            return Integer.parseInt(getField(record, field,
                    String.valueOf(defaultVal)));
        } catch (Exception e) { return defaultVal; }
    }

    private int parseIntWithDefault(CSVRecord record, String field,
                                    int defaultVal, int min, int max) {
        int val = parseInt(record, field, defaultVal);
        if (val < min || val > max) {
            log.warn("Valeur hors limites pour {} : {} → défaut {}",
                    field, val, defaultVal);
            return defaultVal;
        }
        return val;
    }

    private boolean parseBoolean(String val) {
        return "Yes".equalsIgnoreCase(val) || "true".equalsIgnoreCase(val);
    }

    private EGender parseGender(String val) {
        return "Female".equalsIgnoreCase(val) ? EGender.Female : EGender.Male;
    }

    private EMaritalStatus parseMaritalStatus(String val) {
        return switch (val.toLowerCase()) {
            case "married"  -> EMaritalStatus.Married;
            case "divorced" -> EMaritalStatus.Divorced;
            default         -> EMaritalStatus.Single;
        };
    }

    // ✅ Nouvelle méthode — parse le statut de l'employé
    private EStatutEmployee parseStatut(String val) {
        return "TERMINATED".equalsIgnoreCase(val)
                ? EStatutEmployee.TERMINATED
                : EStatutEmployee.ACTIVE;
    }

    private EDepartment parseDepartment(String val) {
        return switch (val) {
            case "DIRECTION_GENERALE"                  -> EDepartment.DIRECTION_GENERALE;
            case "DIRECTION_RESSOURCES_HUMAINES"       -> EDepartment.DIRECTION_RESSOURCES_HUMAINES;
            case "DIRECTION_ADMINISTRATIVE_FINANCIERE" -> EDepartment.DIRECTION_ADMINISTRATIVE_FINANCIERE;
            case "DIRECTION_JURIDIQUE"                 -> EDepartment.DIRECTION_JURIDIQUE;
            case "DIRECTION_TECHNOLOGIQUE"             -> EDepartment.DIRECTION_TECHNOLOGIQUE;
            case "DIRECTION_RELATIONS_OPERATEURS"      -> EDepartment.DIRECTION_RELATIONS_OPERATEURS;
            default                                    -> EDepartment.DIRECTION_SERVICE_CLIENT;
        };
    }

    private EJobRole parseJobRole(String val) {
        return switch (val) {
            case "DIRECTEUR_GENERAL"          -> EJobRole.DIRECTEUR_GENERAL;
            case "ASSISTANT_DIRECTION"        -> EJobRole.ASSISTANT_DIRECTION;
            case "RESPONSABLE_RH"             -> EJobRole.RESPONSABLE_RH;
            case "CHARGE_RECRUTEMENT"         -> EJobRole.CHARGE_RECRUTEMENT;
            case "CHARGE_FORMATION"           -> EJobRole.CHARGE_FORMATION;
            case "DIRECTEUR_FINANCIER"        -> EJobRole.DIRECTEUR_FINANCIER;
            case "COMPTABLE"                  -> EJobRole.COMPTABLE;
            case "CONTROLEUR_GESTION"         -> EJobRole.CONTROLEUR_GESTION;
            case "DIRECTEUR_JURIDIQUE"        -> EJobRole.DIRECTEUR_JURIDIQUE;
            case "JURISTE"                    -> EJobRole.JURISTE;
            case "CONSEILLER_JURIDIQUE"       -> EJobRole.CONSEILLER_JURIDIQUE;
            case "DIRECTEUR_TECHNIQUE"        -> EJobRole.DIRECTEUR_TECHNIQUE;
            case "ARCHITECTE_SYSTEME"         -> EJobRole.ARCHITECTE_SYSTEME;
            case "INGENIEUR_RESEAU"           -> EJobRole.INGENIEUR_RESEAU;
            case "INGENIEUR_TELECOM"          -> EJobRole.INGENIEUR_TELECOM;
            case "TECHNICIEN_RESEAU"          -> EJobRole.TECHNICIEN_RESEAU;
            case "DIRECTEUR_SI"               -> EJobRole.DIRECTEUR_SI;
            case "DEVELOPPEUR"                -> EJobRole.DEVELOPPEUR;
            case "ANALYSTE_SYSTEME"           -> EJobRole.ANALYSTE_SYSTEME;
            case "ADMINISTRATEUR_SYSTEME"     -> EJobRole.ADMINISTRATEUR_SYSTEME;
            case "DATA_ENGINEER"              -> EJobRole.DATA_ENGINEER;
            case "DATA_ANALYST"               -> EJobRole.DATA_ANALYST;
            case "DIRECTEUR_COMMERCIAL"       -> EJobRole.DIRECTEUR_COMMERCIAL;
            case "RESPONSABLE_COMMERCIAL"     -> EJobRole.RESPONSABLE_COMMERCIAL;
            case "COMMERCIAL"                 -> EJobRole.COMMERCIAL;
            case "CHARGE_MARKETING"           -> EJobRole.CHARGE_MARKETING;
            case "DIRECTEUR_SERVICE_CLIENT"   -> EJobRole.DIRECTEUR_SERVICE_CLIENT;
            case "RESPONSABLE_SERVICE_CLIENT" -> EJobRole.RESPONSABLE_SERVICE_CLIENT;
            case "CONSEILLER_CLIENT"          -> EJobRole.CONSEILLER_CLIENT;
            case "SUPERVISEUR_CENTRE_APPEL"   -> EJobRole.SUPERVISEUR_CENTRE_APPEL;
            default                           -> EJobRole.COMMERCIAL;
        };
    }
}