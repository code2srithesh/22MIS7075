package com.example.vehicle_maintence_scheduler.controller;

import com.example.vehicle_maintence_scheduler.logging.LogClient;
import com.example.vehicle_maintence_scheduler.model.VehicleTask;
import com.example.vehicle_maintence_scheduler.service.SchedulerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
public class VehicleController {

    private final SchedulerService schedulerService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${access.token}")
    private String token;

    public VehicleController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "Vehicle Scheduler Running");
    }

    @GetMapping("/schedule")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> schedule() {
        LogClient logger = new LogClient(token);

        try {
            logger.log("backend", "info", "route", "Schedule API called");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> depotResponse = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate
                    .exchange(
                            "http://4.224.186.213/evaluation-service/depots",
                            HttpMethod.GET,
                            entity,
                            Map.class);

            ResponseEntity<Map<String, Object>> vehicleResponse = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate
                    .exchange(
                            "http://4.224.186.213/evaluation-service/vehicles",
                            HttpMethod.GET,
                            entity,
                            Map.class);

            List<Map<String, Object>> depotMaps = (List<Map<String, Object>>) depotResponse.getBody().get("depots");
            List<Map<String, Object>> vehicleMaps = (List<Map<String, Object>>) vehicleResponse.getBody()
                    .get("vehicles");

            List<VehicleTask> tasks = new ArrayList<>();

            for (Map<String, Object> item : vehicleMaps) {
                VehicleTask task = new VehicleTask();
                task.setTaskID((String) item.get("TaskID"));
                task.setDuration((Integer) item.get("Duration"));
                task.setImpact((Integer) item.get("Impact"));
                tasks.add(task);
            }

            List<Map<String, Object>> schedules = new ArrayList<>();

            for (Map<String, Object> depotMap : depotMaps) {
                int depotId = (Integer) depotMap.get("ID");
                int hours = (Integer) depotMap.get("MechanicHours");

                Map<String, Object> result = schedulerService.solve(tasks, hours, logger);
                result.put("depotID", depotId);
                schedules.add(result);
            }

            logger.log("backend", "info", "controller", "Schedule response generated successfully");

            return ResponseEntity.ok(Map.of("schedules", schedules));

        } catch (Exception e) {
            logger.log("backend", "error", "handler", "Schedule generation failed: " + e.getMessage());

            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to generate vehicle schedule",
                    "message", e.getMessage()));
        }
    }
}