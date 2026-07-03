package com.example.logservice.controller;

import com.example.logservice.common.ApiResponse;
import com.example.logservice.dto.MetricsPoint;
import com.example.logservice.dto.MetricsQueryRequest;
import com.example.logservice.service.MetricsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/log/metrics")
public class MetricsController {
    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @PostMapping("/query")
    public ApiResponse<Map<String, Object>> query(@RequestBody MetricsQueryRequest request) {
        List<MetricsPoint> series = metricsService.query(request);
        return ApiResponse.ok(Map.of("metric_series", series, "total", series.size()));
    }
}
