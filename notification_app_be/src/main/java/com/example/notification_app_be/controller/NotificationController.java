package com.example.notification_app_be.controller;

import com.example.notification_app_be.logging.LogClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
public class NotificationController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${access.token}")
    private String token;

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "Notification Backend Running");
    }

    @GetMapping("/priority-notifications")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> getPriorityNotifications(@RequestParam(defaultValue = "10") int n) {
        LogClient logger = new LogClient(token);

        try {
            logger.log("backend", "info", "route", "Priority notifications API called");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity) restTemplate
                    .exchange(
                            "http://4.224.186.213/evaluation-service/notifications",
                            HttpMethod.GET,
                            entity,
                            Map.class);

            List<Map<String, Object>> rawNotifications = (List<Map<String, Object>>) response.getBody()
                    .get("notifications");

            List<Map<String, Object>> sorted = new ArrayList<>(rawNotifications);

            sorted.sort((a, b) -> Long.compare(
                    getScore(b),
                    getScore(a)));

            List<Map<String, Object>> top = sorted.subList(0, Math.min(n, sorted.size()));

            logger.log("backend", "info", "service", "Top priority notifications calculated");

            return ResponseEntity.ok(Map.of(
                    "count", top.size(),
                    "notifications", top));

        } catch (Exception e) {
            logger.log("backend", "error", "handler", "Priority notification failed: " + e.getMessage());

            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to fetch priority notifications",
                    "message", e.getMessage()));
        }
    }

    private long getScore(Map<String, Object> notification) {
        String type = String.valueOf(notification.get("Type"));
        String timestamp = String.valueOf(notification.get("Timestamp"));

        int weight = switch (type) {
            case "Placement" -> 3;
            case "Result" -> 2;
            case "Event" -> 1;
            default -> 0;
        };

        LocalDateTime time = LocalDateTime.parse(
                timestamp,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        long epochSeconds = time.atZone(java.time.ZoneId.systemDefault()).toEpochSecond();

        return weight * 1_000_000_000L + epochSeconds;
    }
}