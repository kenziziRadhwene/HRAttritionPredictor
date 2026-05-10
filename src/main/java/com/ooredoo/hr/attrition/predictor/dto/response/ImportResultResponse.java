package com.ooredoo.hr.attrition.predictor.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ImportResultResponse {
    private int totalLignes;
    private int crees;
    private int misAJour;
    private int ignores;
    private int erreurs;
    private List<String> detailsErreurs;
    private boolean annule;  
}