package com.ooredoo.hr.attrition.predictor.service;

import com.ooredoo.hr.attrition.predictor.dto.request.RegisterRequest;
import com.ooredoo.hr.attrition.predictor.dto.request.UpdateProfileRequest;
import com.ooredoo.hr.attrition.predictor.dto.request.UpdateUserRequest;
import com.ooredoo.hr.attrition.predictor.dto.response.UserResponse;
import com.ooredoo.hr.attrition.predictor.entity.User;
import com.ooredoo.hr.attrition.predictor.enums.EAuditAction;
import com.ooredoo.hr.attrition.predictor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserResponse createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé : " + request.getEmail());
        }

        User user = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .motDePasse(passwordEncoder.encode(request.getMotDePasse()))
                .userRole(request.getUserRole())
                .departement(request.getDepartement())
                .build();

        User saved = userRepository.save(user);

        auditLogService.log(
                EAuditAction.USER_CREATE,
                "users",
                saved.getId(),
                "{\"email\": \"" + saved.getEmail() + "\", \"role\": \""
                        + saved.getUserRole() + "\"}"
        );

        return mapToResponse(saved);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Utilisateur non trouvé avec l'id : " + id));
        return mapToResponse(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException(
                    "Utilisateur non trouvé avec l'id : " + id);
        }
        auditLogService.log(
                EAuditAction.USER_DELETE,
                "users",
                id,
                null
        );
        userRepository.deleteById(id);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Utilisateur non trouvé avec l'id : " + id));

        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setEmail(request.getEmail());
        user.setUserRole(request.getUserRole());
        user.setDepartement(request.getDepartement());

        if (request.getMotDePasse() != null && !request.getMotDePasse().isEmpty()) {
            user.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        }

        User updated = userRepository.save(user);

        auditLogService.log(
                EAuditAction.USER_UPDATE,
                "users",
                updated.getId(),
                "{\"email\": \"" + updated.getEmail() + "\"}"
        );

        return mapToResponse(updated);
    }

    public UserResponse updateProfile(Long id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Utilisateur non trouvé avec l'id : " + id));

        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setEmail(request.getEmail());

        User updated = userRepository.save(user);

        auditLogService.log(
                EAuditAction.PROFILE_UPDATE,
                "users",
                updated.getId(),
                "{\"email\": \"" + updated.getEmail() + "\"}"
        );

        return mapToResponse(updated);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .userRole(user.getUserRole())
                .departement(user.getDepartement())
                .build();
    }
}