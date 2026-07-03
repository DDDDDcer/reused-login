package com.example.logservice.service;

import com.example.logservice.dto.MetricsPoint;
import com.example.logservice.dto.MetricsQueryRequest;
import com.example.logservice.model.AccessLog;
import com.example.logservice.store.InMemoryLogStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MetricsService {
    private final InMemoryLogStore store;

    public MetricsService(InMemoryLogStore store) {
        this.store = store;
    }

    public List<MetricsPoint> query(MetricsQueryRequest request) {
        String metricName = request.getMetricName() == null || request.getMetricName().isBlank()
                ? "request_count"
                : request.getMetricName();
        String groupBy = request.getGroupBy() == null || request.getGroupBy().isBlank()
                ? "service_name"
                : request.getGroupBy();
        List<AccessLog> logs = filteredLogs(request);
        Map<String, List<AccessLog>> groups = logs.stream().collect(Collectors.groupingBy(log -> groupValue(log, groupBy)));
        return groups.entrySet().stream()
                .map(entry -> new MetricsPoint(entry.getKey(), metricName, metricValue(metricName, entry.getValue())))
                .toList();
    }

    private List<AccessLog> filteredLogs(MetricsQueryRequest request) {
        Map<String, String> filters = request.getFilters();
        return store.getAccessLogs().stream()
                .filter(log -> request.getStartTime() == null || !log.getRequestTime().isBefore(request.getStartTime()))
                .filter(log -> request.getEndTime() == null || !log.getRequestTime().isAfter(request.getEndTime()))
                .filter(log -> matchesFilter(filters, "service_name", log.getServiceName()))
                .filter(log -> matchesFilter(filters, "path", log.getPath()))
                .filter(log -> matchesFilter(filters, "status_code", String.valueOf(log.getStatusCode())))
                .toList();
    }

    private String groupValue(AccessLog log, String groupBy) {
        return switch (groupBy) {
            case "path" -> log.getPath();
            case "service_name" -> log.getServiceName();
            default -> "all";
        };
    }

    private double metricValue(String metricName, List<AccessLog> logs) {
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

    private boolean matchesFilter(Map<String, String> filters, String key, String actual) {
        if (filters == null || !filters.containsKey(key)) {
            return true;
        }
        String expected = filters.get(key);
        return expected == null || expected.isBlank() || expected.equals(actual);
    }
}
