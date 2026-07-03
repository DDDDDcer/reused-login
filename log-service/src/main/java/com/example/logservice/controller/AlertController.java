package com.example.logservice.controller;

import com.example.logservice.common.ApiResponse;
import com.example.logservice.dto.AlertRuleRequest;
import com.example.logservice.model.AlertEvent;
import com.example.logservice.model.AlertRule;
import com.example.logservice.service.AlertService;
import com.example.logservice.service.LogQueryService;
import com.example.logservice.store.InMemoryLogStore;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/log/alerts")
public class AlertController {
    private final InMemoryLogStore store;
    private final AlertService alertService;
    private final LogQueryService logQueryService;

    public AlertController(InMemoryLogStore store, AlertService alertService, LogQueryService logQueryService) {
        this.store = store;
        this.alertService = alertService;
        this.logQueryService = logQueryService;
    }

    @PostMapping("/rules")
    public ApiResponse<Map<String, Object>> createRule(@RequestBody AlertRuleRequest request) {
        AlertRule rule = alertService.createRule(request);
        return ApiResponse.ok(Map.of("rule_id", rule.getRuleId(), "create_result", true));
    }

    @GetMapping("/rules")
    public ApiResponse<Map<String, Object>> listRules(@RequestParam(required = false) Boolean enabled) {
        List<AlertRule> rules = store.getAlertRules().stream()
                .filter(rule -> enabled == null || rule.isEnabled() == enabled)
                .toList();
        return ApiResponse.ok(Map.of("rule_list", rules, "total", rules.size()));
    }

    @PutMapping("/rules/{ruleId}")
    public ApiResponse<Map<String, Object>> updateRule(@PathVariable String ruleId, @RequestBody AlertRuleRequest request) {
        return ApiResponse.ok(Map.of("update_result", alertService.updateRule(ruleId, request)));
    }

    @DeleteMapping("/rules/{ruleId}")
    public ApiResponse<Map<String, Object>> deleteRule(@PathVariable String ruleId) {
        return ApiResponse.ok(Map.of("delete_result", store.deleteAlertRule(ruleId)));
    }

    @GetMapping("/history")
    public ApiResponse<Map<String, Object>> history(
            @RequestParam(name = "rule_id", required = false) String ruleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "20") Integer pageSize) {
        List<AlertEvent> matched = alertService.history(ruleId, from, to);
        return ApiResponse.ok(Map.of(
                "alert_event_list", logQueryService.page(matched, page, pageSize),
                "total", matched.size()
        ));
    }
}
