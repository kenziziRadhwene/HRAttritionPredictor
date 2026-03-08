package com.ooredoo.hr.attrition.predictor.dto.response;

import com.ooredoo.hr.attrition.predictor.enums.ERole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private ERole userRole;
}