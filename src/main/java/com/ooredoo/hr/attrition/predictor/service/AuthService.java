package com.ooredoo.hr.attrition.predictor.service;

import com.ooredoo.hr.attrition.predictor.dto.request.LoginRequest;
import com.ooredoo.hr.attrition.predictor.dto.response.AuthResponse;
import com.ooredoo.hr.attrition.predictor.entity.User;
import com.ooredoo.hr.attrition.predictor.enums.EAuditAction;
import com.ooredoo.hr.attrition.predictor.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public AuthResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getMotDePasse()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String token = jwtUtil.generateToken(userDetails);

        // Passer user explicitement — le SecurityContext est vide au moment du login
        auditLogService.log(
                EAuditAction.LOGIN,
                "users",
                user.getId(),
                "{\"email\": \"" + user.getEmail() + "\"}",
                user
        );

        return AuthResponse.builder()
                .token(token)
                .email(userDetails.getUsername())
                .role(userDetails.getAuthorities().iterator().next().getAuthority())
                .departement(user.getDepartement())
                .build();
    }
}