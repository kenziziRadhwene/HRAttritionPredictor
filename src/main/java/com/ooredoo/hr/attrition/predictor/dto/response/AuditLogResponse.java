package com.ooredoo.hr.attrition.predictor.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long          id;
    private String        utilisateur;   // "Nom Prénom"
    private String        email;
    private String        role;
    private String        action;        // EAuditAction.name()
    private String        actionLabel;   // libellé lisible
    private String        targetTable;
    private Long          targetId;
    private String        details;
    private String        ipAddress;
    private LocalDateTime createdAt;
}