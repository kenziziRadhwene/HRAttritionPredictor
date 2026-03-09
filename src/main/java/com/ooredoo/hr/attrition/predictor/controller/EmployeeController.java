package com.ooredoo.hr.attrition.predictor.controller;

import com.ooredoo.hr.attrition.predictor.dto.request.EmployeeRequest;
import com.ooredoo.hr.attrition.predictor.dto.response.EmployeeResponse;
import com.ooredoo.hr.attrition.predictor.service.EmployeeService;
import com.ooredoo.hr.attrition.predictor.service.MLService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final MLService mlService;

    // ─────────────────────────────────────
    // POST /api/employees
    // Créer un employé
    // ─────────────────────────────────────
    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─────────────────────────────────────
    // GET /api/employees
    // Récupérer tous les employés actifs
    // ─────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    // ─────────────────────────────────────
    // GET /api/employees/{id}
    // Récupérer un employé par ID
    // ─────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    // ─────────────────────────────────────
    // PUT /api/employees/{id}
    // Mettre à jour un employé
    // ─────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    // ─────────────────────────────────────
    // DELETE /api/employees/{id}
    // Désactiver un employé (soft delete)
    // ─────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────
    // POST /api/employees/{id}/predict
    // Lancer la prédiction ML pour un employé
    // ─────────────────────────────────────
    @PostMapping("/{id}/predict")
    public ResponseEntity<?> predictRisk(@PathVariable Long id) {
        try {
            var score = mlService.predictAndSave(id);
            return ResponseEntity.ok(score);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur prédiction : " + e.getMessage());
        }
    }
}