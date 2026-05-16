package com.example.vehicle_maintence_scheduler.service;

import com.example.vehicle_maintence_scheduler.logging.LogClient;
import com.example.vehicle_maintence_scheduler.model.VehicleTask;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SchedulerService {

    public Map<String, Object> solve(List<VehicleTask> tasks, int capacity, LogClient logger) {
        logger.log("backend", "info", "service", "Starting vehicle scheduling using knapsack");

        int n = tasks.size();
        int[][] dp = new int[n + 1][capacity + 1];

        for (int i = 1; i <= n; i++) {
            VehicleTask task = tasks.get(i - 1);

            for (int h = 0; h <= capacity; h++) {
                dp[i][h] = dp[i - 1][h];

                if (task.getDuration() <= h) {
                    dp[i][h] = Math.max(
                            dp[i][h],
                            task.getImpact() + dp[i - 1][h - task.getDuration()]);
                }
            }
        }

        List<VehicleTask> selected = new ArrayList<>();
        int h = capacity;

        for (int i = n; i > 0; i--) {
            if (dp[i][h] != dp[i - 1][h]) {
                VehicleTask task = tasks.get(i - 1);
                selected.add(task);
                h -= task.getDuration();
            }
        }

        Collections.reverse(selected);

        int totalDuration = selected.stream().mapToInt(VehicleTask::getDuration).sum();

        logger.log("backend", "info", "service", "Vehicle scheduling completed successfully");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mechanicHours", capacity);
        result.put("totalDuration", totalDuration);
        result.put("maxImpact", dp[n][capacity]);
        result.put("selectedTasks", selected);

        return result;
    }
}