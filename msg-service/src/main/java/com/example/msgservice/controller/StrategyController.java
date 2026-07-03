package com.example.msgservice.controller;

import com.example.msgservice.common.ApiResponse;
import com.example.msgservice.service.StrategyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/message-strategies")
@Tag(name = "Message Strategies")
public class StrategyController {
    private final StrategyService strategyService;

    public StrategyController(StrategyService strategyService) {
        this.strategyService = strategyService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(strategyService.list(status));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(strategyService.create(
                RequestMaps.required(body, "strategy_name", "strategyName"),
                RequestMaps.intValue(body, 0, "max_retries", "maxRetries"),
                RequestMaps.intValue(body, 60, "retry_interval_seconds", "retryIntervalSeconds"),
                RequestMaps.stringValue(body, "status")));
    }

    @PutMapping("/{strategyId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long strategyId,
                                                   @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(strategyService.update(
                strategyId,
                RequestMaps.stringValue(body, "strategy_name", "strategyName"),
                body.containsKey("max_retries") || body.containsKey("maxRetries")
                        ? RequestMaps.intValue(body, 0, "max_retries", "maxRetries") : null,
                body.containsKey("retry_interval_seconds") || body.containsKey("retryIntervalSeconds")
                        ? RequestMaps.intValue(body, 60, "retry_interval_seconds", "retryIntervalSeconds") : null,
                RequestMaps.stringValue(body, "status")));
    }
}
