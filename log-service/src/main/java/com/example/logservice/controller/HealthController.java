package com.example.logservice.controller;

import com.example.logservice.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/log")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "status", "UP",
                "checked_at", LocalDateTime.now(),
                "dependency_status", Map.of(
                        "storage", "memory",
                        "clickhouse", "mocked",
                        "vector", "mocked"
                )
        ));
    }
}
