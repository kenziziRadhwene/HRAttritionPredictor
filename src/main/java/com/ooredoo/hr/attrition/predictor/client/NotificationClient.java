package com.ooredoo.hr.attrition.predictor.client;

import com.ooredoo.hr.attrition.predictor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClient {

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;  // ← AJOUT

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    public void sendRiskReport(List<EmployeeRiskPayload> employees) {
        String url = notificationServiceUrl + "/api/notifications/rapport-risque";

        // ← Récupérer les emails RH depuis la BDD
        List<String> rhEmails = userRepository.findAllResponsableRHEmails();
        log.info("📧 Emails RH trouvés : {}", rhEmails);

        if (rhEmails.isEmpty()) {
            log.warn("⚠️ Aucun RESPONSABLE_RH trouvé en base — rapport non envoyé");
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "employees", employees,
                "rhEmails",  rhEmails   // ← on passe les emails au Mailing Server
        );
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Rapport PDF envoyé au Mailing Server : {}", response.getBody());
            } else {
                log.warn("⚠️ Réponse inattendue du Mailing Server : {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'appel au Mailing Server : {}", e.getMessage());
        }
    }

    public record EmployeeRiskPayload(
            Long    employeeId,
            String  employeeNom,
            String  employeeMatricule,
            String  employeeDepartement,
            String  employeePoste,
            Integer employeeAnciennete,
            Double  probabilite,
            String  niveauRisque,
            String  dateCalcul,
            String  modelVersion
    ) {}
}