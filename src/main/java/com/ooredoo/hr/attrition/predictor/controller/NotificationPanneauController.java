package com.ooredoo.hr.attrition.predictor.controller;

import com.ooredoo.hr.attrition.predictor.dto.response.NotificationResponse;
import com.ooredoo.hr.attrition.predictor.entity.User;
import com.ooredoo.hr.attrition.predictor.repository.UserRepository;
import com.ooredoo.hr.attrition.predictor.service.NotificationPanneauService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationPanneauController {

    private final NotificationPanneauService notificationPanneauService;
    private final UserRepository             userRepository;

    // GET /api/notifications — liste des notifications du RH connecté
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // TEMPORAIRE — à supprimer après debug
        System.out.println(">>> USER ID : " + user.getId());
        System.out.println(">>> EMAIL   : " + user.getEmail());

        return ResponseEntity.ok(
                notificationPanneauService.getNotifications(user.getId()));
    }

    // GET /api/notifications/count — badge
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countNonLues(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        long count = notificationPanneauService.countNonLues(user.getId());
        return ResponseEntity.ok(Map.of("nonLues", count));
    }

    // PUT /api/notifications/marquer-lues — appelé quand le RH ouvre le panneau
    @PutMapping("/marquer-lues")
    public ResponseEntity<Void> marquerToutesLues(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        notificationPanneauService.marquerToutesLues(user.getId());
        return ResponseEntity.ok().build();
    }
}