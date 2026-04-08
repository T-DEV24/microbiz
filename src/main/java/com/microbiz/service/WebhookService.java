package com.microbiz.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class WebhookService {

    @Value("${microbiz.webhook.url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void publish(String eventType, Map<String, Object> payload) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of(
                "event", eventType,
                "payload", payload
        ), headers);

        try {
            restTemplate.postForEntity(webhookUrl, entity, String.class);
        } catch (Exception ignored) {
            // Eviter d'interrompre le flux métier si le webhook est indisponible.
        }
    }
}
