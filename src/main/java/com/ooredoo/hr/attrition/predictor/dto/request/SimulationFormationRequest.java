package com.ooredoo.hr.attrition.predictor.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SimulationFormationRequest {

    @NotNull(message = "L'ID employé est obligatoire")
    private Long employeeId;

    @NotNull(message = "Le nombre de formations est obligatoire")
    @Min(value = 1, message = "Minimum 1 formation")
    @Max(value = 6, message = "Maximum 6 formations par an")
    private Integer nombreFormations;
}