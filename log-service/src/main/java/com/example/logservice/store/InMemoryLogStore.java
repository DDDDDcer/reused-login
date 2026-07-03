package com.example.logservice.store;

import com.example.logservice.model.AccessLog;
import com.example.logservice.model.AlertEvent;
import com.example.logservice.model.AlertRule;
import com.example.logservice.model.CollectionConfig;
import com.example.logservice.model.RetentionPolicy;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryLogStore {
    private final List<AccessLog> accessLogs = new CopyOnWriteArrayList<>();
    private final List<AlertRule> alertRules = new CopyOnWriteArrayList<>();
    private final List<AlertEvent> alertEvents = new CopyOnWriteArrayList<>();
    private CollectionConfig collectionConfig;
    private RetentionPolicy retentionPolicy;

    @PostConstruct
    public void init() {
        reset();
    }

    public synchronized void reset() {
        accessLogs.clear();
        alertRules.clear();
        alertEvents.clear();
        initConfig();
        initLogs();
        initRules();
    }

    public List<AccessLog> getAccessLogs() {
        return Collections.unmodifiableList(new ArrayList<>(accessLogs));
    }

    public void addAccessLog(AccessLog accessLog) {
        accessLogs.add(accessLog);
    }

    public int removeAccessLogsBefore(LocalDateTime cutoff) {
        int before = accessLogs.size();
        accessLogs.removeIf(log -> log.getRequestTime() != null && log.getRequestTime().isBefore(cutoff));
        return before - accessLogs.size();
    }

    public List<AlertRule> getAlertRules() {
        return Collections.unmodifiableList(new ArrayList<>(alertRules));
    }

    public void addAlertRule(AlertRule alertRule) {
        alertRules.add(alertRule);
    }

    public boolean replaceAlertRule(String ruleId, AlertRule replacement) {
        for (int i = 0; i < alertRules.size(); i++) {
            if (alertRules.get(i).getRuleId().equals(ruleId)) {
                alertRules.set(i, replacement);
                return true;
            }
        }
        return false;
    }

    public boolean deleteAlertRule(String ruleId) {
        return alertRules.removeIf(rule -> rule.getRuleId().equals(ruleId));
    }

    public List<AlertEvent> getAlertEvents() {
        return Collections.unmodifiableList(new ArrayList<>(alertEvents));
    }

    public void addAlertEvent(AlertEvent alertEvent) {
        alertEvents.add(alertEvent);
    }

    public CollectionConfig getCollectionConfig() {
        return collectionConfig;
    }

    public RetentionPolicy getRetentionPolicy() {
        return retentionPolicy;
    }

    public void setRetentionPolicy(RetentionPolicy retentionPolicy) {
        this.retentionPolicy = retentionPolicy;
    }

    private void initConfig() {
        collectionConfig = new CollectionConfig();
        collectionConfig.setLogPath("./logs/topbiz-access.log");
        collectionConfig.setFormat("json");
        collectionConfig.setEnabled(true);
        collectionConfig.setBatchSize(100);
        collectionConfig.setFlushInterval("10s");
        collectionConfig.setTargetStorage("memory-clickhouse-mock");

        retentionPolicy = new RetentionPolicy();
        retentionPolicy.setRetentionDays(30);
        retentionPolicy.setCleanupCycle("daily");
        retentionPolicy.setEnabled(true);
    }

    private void initLogs() {
        LocalDateTime now = LocalDateTime.now();
        accessLogs.add(log(now.minusMinutes(50), "u001", "10.0.0.11", "topbiz-order", "/api/orders", "GET", 200, 86, "trace-1001", "query orders ok"));
        accessLogs.add(log(now.minusMinutes(44), "u002", "10.0.0.12", "topbiz-order", "/api/orders", "POST", 201, 142, "trace-1002", "create order ok"));
        accessLogs.add(log(now.minusMinutes(38), "u003", "10.0.0.13", "topbiz-order", "/api/orders/404", "GET", 404, 39, "trace-1003", "order not found"));
        accessLogs.add(log(now.minusMinutes(32), "u004", "10.0.0.14", "topbiz-user", "/api/users/profile", "GET", 200, 64, "trace-1004", "profile ok"));
        accessLogs.add(log(now.minusMinutes(25), "u005", "10.0.0.15", "topbiz-user", "/api/users/login", "POST", 401, 55, "trace-1005", "bad credential"));
        accessLogs.add(log(now.minusMinutes(18), "u006", "10.0.0.16", "topbiz-payment", "/api/payments", "POST", 500, 760, "trace-1006", "payment provider timeout"));
        accessLogs.add(log(now.minusMinutes(10), "u007", "10.0.0.17", "topbiz-payment", "/api/payments", "POST", 502, 690, "trace-1007", "gateway error"));
        accessLogs.add(log(now.minusMinutes(3), "u008", "10.0.0.18", "topbiz-report", "/api/reports/daily", "GET", 200, 320, "trace-1008", "daily report ok"));
    }

    private void initRules() {
        AlertRule errorRate = new AlertRule();
        errorRate.setRuleId("rule-" + UUID.randomUUID());
        errorRate.setRuleName("错误率超过阈值");
        errorRate.setMetricName("error_rate");
        errorRate.setOperator(">=");
        errorRate.setThreshold(0.3);
        errorRate.setDuration("5m");
        errorRate.setEnabled(true);
        errorRate.setNotifyType("console");
        alertRules.add(errorRate);

        AlertRule avgCost = new AlertRule();
        avgCost.setRuleId("rule-" + UUID.randomUUID());
        avgCost.setRuleName("平均耗时超过阈值");
        avgCost.setMetricName("avg_cost_ms");
        avgCost.setOperator(">=");
        avgCost.setThreshold(500);
        avgCost.setDuration("5m");
        avgCost.setEnabled(true);
        avgCost.setNotifyType("console");
        alertRules.add(avgCost);
    }

    private AccessLog log(LocalDateTime requestTime, String userId, String clientIp, String serviceName,
                          String path, String method, int statusCode, long costMs, String traceId, String message) {
        AccessLog log = new AccessLog();
        log.setLogId("log-" + UUID.randomUUID());
        log.setRequestTime(requestTime);
        log.setUserId(userId);
        log.setClientIp(clientIp);
        log.setServiceName(serviceName);
        log.setPath(path);
        log.setMethod(method);
        log.setStatusCode(statusCode);
        log.setCostMs(costMs);
        log.setTraceId(traceId);
        log.setLevel(statusCode >= 500 ? "ERROR" : statusCode >= 400 ? "WARN" : "INFO");
        log.setMessage(message);
        return log;
    }
}
