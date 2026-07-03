package com.example.logservice.dto;

public class RetentionPolicyRequest {
    private Integer retentionDays;
    private String cleanupCycle;
    private Boolean enabled;

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getCleanupCycle() {
        return cleanupCycle;
    }

    public void setCleanupCycle(String cleanupCycle) {
        this.cleanupCycle = cleanupCycle;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
