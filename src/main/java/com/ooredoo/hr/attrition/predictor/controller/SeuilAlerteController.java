package com.ooredoo.hr.attrition.predictor.controller;

import com.ooredoo.hr.attrition.predictor.dto.request.SeuilAlerteRequest;
import com.ooredoo.hr.attrition.predictor.dto.response.SeuilAlerteResponse;
import com.ooredoo.hr.attrition.predictor.service.SeuilAlerteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seuils")
@RequiredArgsConstructor
public class SeuilAlerteController {

    private final SeuilAlerteService seuilAlerteService;

    // ─────────────────────────────────────
    // POST /api/seuils
    // Créer un nouveau seuil
    // ─────────────────────────────────────
    @PostMapping
    public ResponseEntity<SeuilAlerteResponse> createSeuil(
            @Valid @RequestBody SeuilAlerteRequest request) {
        return ResponseEntity.ok(seuilAlerteService.createSeuil(request));
    }

    // ─────────────────────────────────────
    // GET /api/seuils
    // Récupérer tous les seuils
    // ─────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<SeuilAlerteResponse>> getAllSeuils() {
        return ResponseEntity.ok(seuilAlerteService.getAllSeuils());
    }

    // ─────────────────────────────────────
    // GET /api/seuils/{id}
    // Récupérer un seuil par ID
    // ─────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<SeuilAlerteResponse> getSeuilById(@PathVariable Long id) {
        return ResponseEntity.ok(seuilAlerteService.getSeuilById(id));
    }

    // ─────────────────────────────────────
    // PUT /api/seuils/{id}
    // Mettre à jour un seuil
    // ─────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<SeuilAlerteResponse> updateSeuil(
            @PathVariable Long id,
            @Valid @RequestBody SeuilAlerteRequest request) {
        return ResponseEntity.ok(seuilAlerteService.updateSeuil(id, request));
    }

    // ─────────────────────────────────────
    // PUT /api/seuils/{id}/toggle
    // Activer / Désactiver un seuil
    // ─────────────────────────────────────
    @PutMapping("/{id}/toggle")
    public ResponseEntity<SeuilAlerteResponse> toggleActif(@PathVariable Long id) {
        return ResponseEntity.ok(seuilAlerteService.toggleActif(id));
    }

    // ─────────────────────────────────────
    // DELETE /api/seuils/{id}
    // Supprimer un seuil
    // ─────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeuil(@PathVariable Long id) {
        seuilAlerteService.deleteSeuil(id);
        return ResponseEntity.noContent().build();
    }
}