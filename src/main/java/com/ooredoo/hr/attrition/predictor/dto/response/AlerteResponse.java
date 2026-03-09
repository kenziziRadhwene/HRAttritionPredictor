package com.ooredoo.hr.attrition.predictor.dto.response;

import com.ooredoo.hr.attrition.predictor.enums.EStatutAlerte;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlerteResponse {

    private Long id;
    private String titre;
    private String message;
    private Double probabilite;
    private EStatutAlerte statut;
    private String emailDestinataire;
    private Boolean emailEnvoye;
    private LocalDateTime dateEnvoi;
    private LocalDateTime dateCreation;
    private Long employeeId;
    private String employeeNom;
    private String employeeMatricule;
}