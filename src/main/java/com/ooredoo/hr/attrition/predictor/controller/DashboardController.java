package com.ooredoo.hr.attrition.predictor.controller;

import com.ooredoo.hr.attrition.predictor.dto.response.DashboardStatsResponse;
import com.ooredoo.hr.attrition.predictor.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // ─────────────────────────────────────
    // GET /api/dashboard/stats
    // US16 — Statistiques globales
    // ─────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }
    // GET /api/dashboard/stats/filtered
    @GetMapping("/stats/filtered")
    public ResponseEntity<DashboardStatsResponse> getStatsFiltered(
            @RequestParam(required = false) String departement,
            @RequestParam(required = false) String niveauRisque) {
        return ResponseEntity.ok(dashboardService.getStatsFiltered(departement, niveauRisque));
    }


}