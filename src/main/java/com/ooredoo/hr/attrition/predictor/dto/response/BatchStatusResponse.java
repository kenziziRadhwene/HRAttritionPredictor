package com.ooredoo.hr.attrition.predictor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStatusResponse {
    private boolean isRunning;
    private LocalDateTime lastExecutionTime;
    private int lastTotalEmployees;
    private int lastSuccessCount;
    private int lastFailedCount;
}