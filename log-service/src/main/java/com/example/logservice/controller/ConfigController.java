package com.example.logservice.controller;

import com.example.logservice.common.ApiResponse;
import com.example.logservice.dto.RetentionPolicyRequest;
import com.example.logservice.model.RetentionPolicy;
import com.example.logservice.service.ConfigService;
import com.example.logservice.store.InMemoryLogStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/log/config")
public class ConfigController {
    private final InMemoryLogStore store;
    private final ConfigService configService;

    public ConfigController(InMemoryLogStore store, ConfigService configService) {
        this.store = store;
        this.configService = configService;
    }

    @GetMapping("/retention")
    public ApiResponse<RetentionPolicy> retention() {
        return ApiResponse.ok(store.getRetentionPolicy());
    }

    @PutMapping("/retention")
    public ApiResponse<Map<String, Object>> updateRetention(@RequestBody RetentionPolicyRequest request) {
        configService.updateRetention(request);
        return ApiResponse.ok(Map.of("update_result", true));
    }
}
