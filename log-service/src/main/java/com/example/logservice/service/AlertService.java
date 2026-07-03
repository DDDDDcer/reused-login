package com.example.logservice.service;

import com.example.logservice.dto.AlertRuleRequest;
import com.example.logservice.model.AccessLog;
import com.example.logservice.model.AlertEvent;
import com.example.logservice.model.AlertRule;
import com.example.logservice.store.InMemoryLogStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AlertService {
    private final InMemoryLogStore store;

    public AlertService(InMemoryLogStore store) {
        this.store = store;
    }

    public AlertRule createRule(AlertRuleRequest request) {
        AlertRule rule = toRule(request, "rule-" + UUID.randomUUID());
        store.addAlertRule(rule);
        return rule;
    }

    public boolean updateRule(String ruleId, AlertRuleRequest request) {
        return store.replaceAlertRule(ruleId, toRule(request, ruleId));
    }

    public void checkRules(AccessLog latestLog) {
        for (AlertRule rule : store.getAlertRules()) {
            if (!rule.isEnabled()) {
                continue;
            }
            double value = metricValue(rule.getMetricName(), latestLog.getServiceName());
            if (compare(value, rule.getOperator(), rule.getThreshold())) {
                AlertEvent event = new AlertEvent();
                event.setEventId("event-" + UUID.randomUUID());
                event.setRuleId(rule.getRuleId());
                event.setRuleName(rule.getRuleName());
                event.setServiceName(latestLog.getServiceName());
                event.setTriggeredAt(LocalDateTime.now());
                event.setMetricName(rule.getMetricName());
                event.setMetricValue(value);
                event.setThreshold(rule.getThreshold());
                event.setReason(rule.getMetricName() + " " + rule.getOperator() + " " + rule.getThreshold());
                store.addAlertEvent(event);
            }
        }
    }

    public List<AlertEvent> history(String ruleId, LocalDateTime from, LocalDateTime to) {
        return store.getAlertEvents().stream()
                .filter(event -> ruleId == null || ruleId.isBlank() || ruleId.equals(event.getRuleId()))
                .filter(event -> from == null || !event.getTriggeredAt().isBefore(from))
                .filter(event -> to == null || !event.getTriggeredAt().isAfter(to))
                .sorted((a, b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
                .toList();
    }

    private AlertRule toRule(AlertRuleRequest request, String ruleId) {
        AlertRule rule = new AlertRule();
        rule.setRuleId(ruleId);
        rule.setRuleName(defaultText(request.getRuleName(), "unnamed rule"));
        rule.setMetricName(defaultText(request.getMetricName(), "error_count"));
        rule.setOperator(defaultText(request.getOperator(), ">="));
        rule.setThreshold(request.getThreshold() == null ? 1 : request.getThreshold());
        rule.setDuration(defaultText(request.getDuration(), "5m"));
        rule.setEnabled(request.getEnabled() == null || request.getEnabled());
        rule.setNotifyType(defaultText(request.getNotifyType(), "console"));
        return rule;
    }

    private double metricValue(String metricName, String serviceName) {
        List<AccessLog> logs = store.getAccessLogs().stream()
                .filter(log -> serviceName == null || serviceName.equals(log.getServiceName()))
                .toList();
        if (logs.isEmpty()) {
            return 0;
        }
        long errors = logs.stream().filter(log -> log.getStatusCode() >= 400).count();
        return switch (metricName) {
            case "request_count" -> logs.size();
            case "error_count" -> errors;
            case "error_rate" -> (double) errors / logs.size();
            case "avg_cost_ms" -> logs.stream().mapToLong(AccessLog::getCostMs).average().orElse(0);
            default -> 0;
        };
    }

    private boolean compare(double value, String operator, double threshold) {
        return switch (operator) {
            case ">" -> value > threshold;
            case ">=" -> value >= threshold;
            case "<" -> value < threshold;
            case "<=" -> value <= threshold;
            case "==" -> Double.compare(value, threshold) == 0;
            default -> false;
        };
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
