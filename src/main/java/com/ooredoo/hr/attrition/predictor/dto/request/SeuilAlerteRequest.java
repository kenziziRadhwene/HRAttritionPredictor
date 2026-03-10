package com.ooredoo.hr.attrition.predictor.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SeuilAlerteRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotNull(message = "Le seuil minimum est obligatoire")
    @DecimalMin(value = "0.0") @DecimalMax(value = "1.0")
    private Double seuilMinimum;

    @NotNull(message = "Le seuil maximum est obligatoire")
    @DecimalMin(value = "0.0") @DecimalMax(value = "1.0")
    private Double seuilMaximum;

    private Boolean actif = true;
    private Boolean envoyerEmail = true;

    @NotBlank(message = "L'email destinataire est obligatoire")
    private String emailsDestinataires;

    @NotBlank(message = "Le créateur est obligatoire")
    private String createdBy;
}