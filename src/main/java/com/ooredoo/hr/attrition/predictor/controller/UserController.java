package com.ooredoo.hr.attrition.predictor.controller;

import com.ooredoo.hr.attrition.predictor.dto.request.RegisterRequest;
import com.ooredoo.hr.attrition.predictor.dto.request.UpdateProfileRequest;
import com.ooredoo.hr.attrition.predictor.dto.request.UpdateUserRequest;
import com.ooredoo.hr.attrition.predictor.dto.response.UserResponse;
import com.ooredoo.hr.attrition.predictor.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // US4 — Créer un utilisateur (ADMIN uniquement)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    // US4 — Lister tous les utilisateurs (ADMIN uniquement)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // US4 — Récupérer un utilisateur par ID (ADMIN uniquement)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // US4 — Supprimer un utilisateur (ADMIN uniquement)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // US5 — Modifier son propre profil (tous les rôles)
    @PutMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_RH', 'MANAGER')")
    public ResponseEntity<UserResponse> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(id, request));
    }

    // US4 — Modifier un utilisateur (ADMIN uniquement)

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }
}