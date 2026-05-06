package com.ooredoo.hr.attrition.predictor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPredictionResponse {

    private int totalEmployees;
    private int successCount;
    private int failedCount;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    private long durationMs;
}