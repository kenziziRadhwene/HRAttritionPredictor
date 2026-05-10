package com.ooredoo.hr.attrition.predictor.dto.request;

import com.ooredoo.hr.attrition.predictor.enums.ERole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    // Optionnel pour la modification
    private String motDePasse;

    @NotNull(message = "Le rôle est obligatoire")
    private ERole userRole;

    private String departement;
}