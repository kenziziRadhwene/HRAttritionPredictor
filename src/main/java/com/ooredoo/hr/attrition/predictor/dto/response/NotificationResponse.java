package com.ooredoo.hr.attrition.predictor.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long          id;
    private String        type;
    private String        message;
    private String        valeur;
    private boolean       lue;
    private String        moisRecalcul;
    private LocalDateTime dateCreation;
}