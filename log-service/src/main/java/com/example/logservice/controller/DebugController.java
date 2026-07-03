package com.example.logservice.controller;

import com.example.logservice.common.ApiResponse;
import com.example.logservice.service.ConfigService;
import com.example.logservice.store.InMemoryLogStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {
    private final InMemoryLogStore store;
    private final ConfigService configService;

    public DebugController(InMemoryLogStore store, ConfigService configService) {
        this.store = store;
        this.configService = configService;
    }

    @GetMapping("/logs")
    public ApiResponse<Map<String, Object>> logs() {
        return ApiResponse.ok(Map.of(
                "access_logs", store.getAccessLogs(),
                "alert_rules", store.getAlertRules(),
                "alert_events", store.getAlertEvents(),
                "collection_config", store.getCollectionConfig(),
                "retention_policy", store.getRetentionPolicy()
        ));
    }

    @PostMapping("/reset")
    public ApiResponse<Map<String, Object>> reset() {
        store.reset();
        return ApiResponse.ok(Map.of("reset_result", true));
    }

    @PostMapping("/retention/cleanup")
    public ApiResponse<Map<String, Object>> cleanup() {
        return ApiResponse.ok(Map.of("cleanup_count", configService.cleanup()));
    }
}
