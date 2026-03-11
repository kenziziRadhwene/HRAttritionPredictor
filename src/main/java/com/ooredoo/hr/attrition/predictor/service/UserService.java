package com.ooredoo.hr.attrition.predictor.service;

import com.ooredoo.hr.attrition.predictor.dto.request.RegisterRequest;
import com.ooredoo.hr.attrition.predictor.dto.request.UpdateProfileRequest;
import com.ooredoo.hr.attrition.predictor.dto.request.UpdateUserRequest;
import com.ooredoo.hr.attrition.predictor.dto.response.UserResponse;
import com.ooredoo.hr.attrition.predictor.entity.User;
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

    // US4 — Créer un utilisateur
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
                .build();

        User saved = userRepository.save(user);
        return mapToResponse(saved);
    }

    // US4 — Lister tous les utilisateurs
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // US4 — Récupérer un utilisateur par ID
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id : " + id));
        return mapToResponse(user);
    }

    // US4 — Supprimer un utilisateur
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur non trouvé avec l'id : " + id);
        }
        userRepository.deleteById(id);
    }


    // US4 — Modifier un utilisateur (ADMIN)


    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id : " + id));

        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setEmail(request.getEmail());
        user.setUserRole(request.getUserRole());

        if (request.getMotDePasse() != null && !request.getMotDePasse().isEmpty()) {
            user.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        }

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    // US5 — Modifier son propre profil
    public UserResponse updateProfile(Long id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id : " + id));

        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setEmail(request.getEmail());

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    // Mapper User → UserResponse (sans mot de passe)
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .userRole(user.getUserRole())
                .build();
    }
}