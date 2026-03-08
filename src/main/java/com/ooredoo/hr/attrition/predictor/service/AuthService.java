package com.ooredoo.hr.attrition.predictor.service;

import com.ooredoo.hr.attrition.predictor.dto.request.LoginRequest;
import com.ooredoo.hr.attrition.predictor.dto.response.AuthResponse;
import com.ooredoo.hr.attrition.predictor.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest request) {

        // Authentifier l'utilisateur via Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getMotDePasse()
                )
        );

        // Récupérer les détails de l'utilisateur authentifié
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Générer le token JWT
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .email(userDetails.getUsername())
                .role(userDetails.getAuthorities()
                        .iterator().next().getAuthority())
                .build();
    }
}