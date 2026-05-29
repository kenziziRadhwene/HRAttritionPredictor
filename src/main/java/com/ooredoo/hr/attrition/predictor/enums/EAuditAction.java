package com.ooredoo.hr.attrition.predictor.enums;

public enum EAuditAction {
    // Auth
    LOGIN,
    LOGOUT,

    // Utilisateurs
    USER_CREATE,
    USER_UPDATE,
    USER_DELETE,
    PROFILE_UPDATE,

    // Employés
    EMPLOYEE_CREATE,
    EMPLOYEE_UPDATE,
    EMPLOYEE_DELETE,
    EMPLOYEE_IMPORT,        // lié à ImportSession

    // Prédictions
    PREDICTION_INDIVIDUELLE,
    PREDICTION_BATCH,
    PREDICTION_BATCH_DEPARTEMENT,

    // Simulations
    SIMULATION_SALAIRE,
    SIMULATION_POSTE,
    SIMULATION_FORMATION
}
