package com.ooredoo.hr.attrition.predictor.controller;

import com.ooredoo.hr.attrition.predictor.dto.response.AlerteResponse;
import com.ooredoo.hr.attrition.predictor.service.AlerteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertes")
@RequiredArgsConstructor
public class AlerteController {

    private final AlerteService alerteService;

    // ─────────────────────────────────────
    // GET /api/alertes
    // Récupérer toutes les alertes
    // ─────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<AlerteResponse>> getAllAlertes() {
        return ResponseEntity.ok(alerteService.getAllAlertes());
    }

    // ─────────────────────────────────────
    // GET /api/alertes/non-lues
    // Récupérer les alertes non lues
    // ─────────────────────────────────────
    @GetMapping("/non-lues")
    public ResponseEntity<List<AlerteResponse>> getAlertesNonLues() {
        return ResponseEntity.ok(alerteService.getAlertesNonLues());
    }

    // ─────────────────────────────────────
    // PUT /api/alertes/{id}/lue
    // Marquer une alerte comme lue
    // ─────────────────────────────────────
    @PutMapping("/{id}/lue")
    public ResponseEntity<AlerteResponse> marquerCommeLue(@PathVariable Long id) {
        return ResponseEntity.ok(alerteService.marquerCommeLue(id));
    }

    // ─────────────────────────────────────
    // PUT /api/alertes/{id}/traitee
    // Marquer une alerte comme traitée
    // ─────────────────────────────────────
    @PutMapping("/{id}/traitee")
    public ResponseEntity<AlerteResponse> marquerCommeTraitee(@PathVariable Long id) {
        return ResponseEntity.ok(alerteService.marquerCommeTraitee(id));
    }
}