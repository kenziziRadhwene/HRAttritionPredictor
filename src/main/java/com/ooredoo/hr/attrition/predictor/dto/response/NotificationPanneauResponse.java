package com.ooredoo.hr.attrition.predictor.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class NotificationPanneauResponse {
    private NotifItem recalculTermine;
    private NotifItem risquesCritiques;
    private NotifItem rapportDisponible;
    private NotifItem prochainRecalcul;

    @Data
    @Builder
    public static class NotifItem {
        private String type;       // "recalcul" | "critique" | "rapport" | "planifie"
        private String message;
        private Object valeur;     // nombre, date, etc.
        private LocalDateTime date;
        private boolean lue;
    }
}