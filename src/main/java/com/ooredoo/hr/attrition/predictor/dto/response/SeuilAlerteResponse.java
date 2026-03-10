package com.ooredoo.hr.attrition.predictor.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder
public class SeuilAlerteResponse {
    private Long id;
    private String nom;
    private String description;
    private Double seuilMinimum;
    private Double seuilMaximum;
    private Boolean actif;
    private Boolean envoyerEmail;
    private String emailsDestinataires;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}