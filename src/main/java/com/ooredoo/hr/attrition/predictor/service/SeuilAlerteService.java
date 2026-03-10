package com.ooredoo.hr.attrition.predictor.service;

import com.ooredoo.hr.attrition.predictor.dto.request.SeuilAlerteRequest;
import com.ooredoo.hr.attrition.predictor.dto.response.SeuilAlerteResponse;
import com.ooredoo.hr.attrition.predictor.entity.SeuilAlerte;
import com.ooredoo.hr.attrition.predictor.repository.SeuilAlerteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeuilAlerteService {

    private final SeuilAlerteRepository seuilAlerteRepository;

    // ─────────────────────────────────────
    // Créer un seuil
    // ─────────────────────────────────────
    public SeuilAlerteResponse createSeuil(SeuilAlerteRequest request) {
        SeuilAlerte seuil = SeuilAlerte.builder()
                .nom(request.getNom())
                .description(request.getDescription())
                .seuilMinimum(request.getSeuilMinimum())
                .seuilMaximum(request.getSeuilMaximum())
                .actif(request.getActif())
                .envoyerEmail(request.getEnvoyerEmail())
                .emailsDestinataires(request.getEmailsDestinataires())
                .createdBy(request.getCreatedBy())
                .build();
        return toResponse(seuilAlerteRepository.save(seuil));
    }

    // ─────────────────────────────────────
    // Récupérer tous les seuils
    // ─────────────────────────────────────
    public List<SeuilAlerteResponse> getAllSeuils() {
        return seuilAlerteRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────
    // Récupérer un seuil par ID
    // ─────────────────────────────────────
    public SeuilAlerteResponse getSeuilById(Long id) {
        SeuilAlerte seuil = seuilAlerteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seuil non trouvé : " + id));
        return toResponse(seuil);
    }

    // ─────────────────────────────────────
    // Mettre à jour un seuil
    // ─────────────────────────────────────
    public SeuilAlerteResponse updateSeuil(Long id, SeuilAlerteRequest request) {
        SeuilAlerte seuil = seuilAlerteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seuil non trouvé : " + id));
        seuil.setNom(request.getNom());
        seuil.setDescription(request.getDescription());
        seuil.setSeuilMinimum(request.getSeuilMinimum());
        seuil.setSeuilMaximum(request.getSeuilMaximum());
        seuil.setActif(request.getActif());
        seuil.setEnvoyerEmail(request.getEnvoyerEmail());
        return toResponse(seuilAlerteRepository.save(seuil));
    }

    // ─────────────────────────────────────
    // Activer / Désactiver un seuil
    // ─────────────────────────────────────
    public SeuilAlerteResponse toggleActif(Long id) {
        SeuilAlerte seuil = seuilAlerteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seuil non trouvé : " + id));
        seuil.setActif(!seuil.getActif());
        return toResponse(seuilAlerteRepository.save(seuil));
    }

    // ─────────────────────────────────────
    // Supprimer un seuil
    // ─────────────────────────────────────
    public void deleteSeuil(Long id) {
        SeuilAlerte seuil = seuilAlerteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seuil non trouvé : " + id));
        seuilAlerteRepository.delete(seuil);
    }

    // ─────────────────────────────────────
    // Mapper Entity → DTO
    // ─────────────────────────────────────
    private SeuilAlerteResponse toResponse(SeuilAlerte seuil) {
        return SeuilAlerteResponse.builder()
                .id(seuil.getId())
                .nom(seuil.getNom())
                .description(seuil.getDescription())
                .seuilMinimum(seuil.getSeuilMinimum())
                .seuilMaximum(seuil.getSeuilMaximum())
                .actif(seuil.getActif())
                .envoyerEmail(seuil.getEnvoyerEmail())
                .emailsDestinataires(seuil.getEmailsDestinataires())
                .createdBy(seuil.getCreatedBy())
                .createdAt(seuil.getCreatedAt())
                .updatedAt(seuil.getUpdatedAt())
                .build();
    }
}