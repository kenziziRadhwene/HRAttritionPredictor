package com.ooredoo.hr.attrition.predictor.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SimulationSalaireRequest {

    @NotNull(message = "L'ID employé est obligatoire")
    private Long employeeId;

    @NotNull(message = "Le pourcentage d'augmentation est obligatoire")
    @DecimalMin(value = "1.0", message = "L'augmentation minimum est 1%")
    @DecimalMax(value = "100.0", message = "L'augmentation maximum est 100%")
    private Double pourcentageAugmentation;
}