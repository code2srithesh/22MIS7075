package com.example.notification_app_be.logging;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class LogClient {

    private static final String LOG_URL = "http://4.224.186.213/evaluation-service/logs";

    private static final List<String> STACKS = List.of("backend", "frontend");
    private static final List<String> LEVELS = List.of("debug", "info", "warn", "error", "fatal");

    private static final List<String> BACKEND_PACKAGES = List.of(
            "cache", "controller", "cron_job", "db", "domain",
            "handler", "repository", "route", "service",
            "auth", "config", "middleware", "utils");

    private static final List<String> FRONTEND_PACKAGES = List.of(
            "api", "component", "hook", "page", "state",
            "style", "auth", "config", "middleware", "utils");

    private final RestTemplate restTemplate = new RestTemplate();
    private final String token;

    public LogClient(String token) {
        this.token = token;
    }

    public void log(String stack, String level, String pkg, String message) {
        try {
            stack = stack.toLowerCase();
            level = level.toLowerCase();
            pkg = pkg.toLowerCase();

            if (!STACKS.contains(stack))
                return;
            if (!LEVELS.contains(level))
                return;

            if (stack.equals("backend") && !BACKEND_PACKAGES.contains(pkg))
                return;
            if (stack.equals("frontend") && !FRONTEND_PACKAGES.contains(pkg))
                return;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            Map<String, String> body = Map.of(
                    "stack", stack,
                    "level", level,
                    "package", pkg,
                    "message", message);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(LOG_URL, request, String.class);

        } catch (Exception e) {
            System.out.println("Logging failed: " + e.getMessage());
        }
    }
}
