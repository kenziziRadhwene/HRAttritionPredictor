package com.ooredoo.hr.attrition.predictor.dto.request;

import com.ooredoo.hr.attrition.predictor.enums.EDepartment;
import com.ooredoo.hr.attrition.predictor.enums.EJobRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SimulationPosteRequest {

    @NotNull(message = "L'ID employé est obligatoire")
    private Long employeeId;

    @NotNull(message = "Le nouveau poste est obligatoire")
    private EJobRole nouveauPoste;

    @NotNull(message = "Le nouveau département est obligatoire")
    private EDepartment nouveauDepartement;

    private Integer nouveauJobLevel;
}