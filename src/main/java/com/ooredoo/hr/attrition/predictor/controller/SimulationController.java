package com.ooredoo.hr.attrition.predictor.controller;

import com.ooredoo.hr.attrition.predictor.dto.request.SimulationPosteRequest;
import com.ooredoo.hr.attrition.predictor.dto.request.SimulationSalaireRequest;
import com.ooredoo.hr.attrition.predictor.dto.response.ComparaisonSimulationResponse;
import com.ooredoo.hr.attrition.predictor.dto.response.SimulationResponse;
import com.ooredoo.hr.attrition.predictor.service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ooredoo.hr.attrition.predictor.dto.request.SimulationFormationRequest;


@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    // ─────────────────────────────────────
    // POST /api/simulations/salaire
    // US12 — Simuler impact augmentation salaire
    // ─────────────────────────────────────
    @PostMapping("/salaire")
    public ResponseEntity<SimulationResponse> simulerSalaire(
            @Valid @RequestBody SimulationSalaireRequest request) {
        return ResponseEntity.ok(simulationService.simulerAugmentationSalaire(request));
    }



    // ─────────────────────────────────────
// POST /api/simulations/poste
// US13 — Simuler impact changement de poste
// ─────────────────────────────────────
    @PostMapping("/poste")
    public ResponseEntity<SimulationResponse> simulerPoste(
            @Valid @RequestBody SimulationPosteRequest request) {
        return ResponseEntity.ok(simulationService.simulerChangementPoste(request));
    }



    // ─────────────────────────────────────
// POST /api/simulations/formation
// US14 — Simuler impact formation
// ─────────────────────────────────────
    @PostMapping("/formation")
    public ResponseEntity<SimulationResponse> simulerFormation(
            @Valid @RequestBody SimulationFormationRequest request) {
        return ResponseEntity.ok(simulationService.simulerFormation(request));
    }


    // ─────────────────────────────────────
// GET /api/simulations/comparer/{employeeId}
// US15 — Comparer tous les scénarios
// ─────────────────────────────────────
    @GetMapping("/comparer/{employeeId}")
    public ResponseEntity<ComparaisonSimulationResponse> comparerScenarios(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(simulationService.comparerScenarios(employeeId));
    }



}