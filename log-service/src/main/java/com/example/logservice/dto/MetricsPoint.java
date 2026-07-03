package com.example.logservice.dto;

public class MetricsPoint {
    private String group;
    private String metricName;
    private double value;

    public MetricsPoint(String group, String metricName, double value) {
        this.group = group;
        this.metricName = metricName;
        this.value = value;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
