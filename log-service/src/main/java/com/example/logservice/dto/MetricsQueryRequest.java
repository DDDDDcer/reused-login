package com.example.logservice.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class MetricsQueryRequest {
    private String metricName;
    private String groupBy;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String timeGranularity;
    private Map<String, String> filters = new HashMap<>();

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getTimeGranularity() {
        return timeGranularity;
    }

    public void setTimeGranularity(String timeGranularity) {
        this.timeGranularity = timeGranularity;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, String> filters) {
        this.filters = filters == null ? new HashMap<>() : filters;
    }
}
