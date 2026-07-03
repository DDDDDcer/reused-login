package com.example.logservice.model;

public class RetentionPolicy {
    private int retentionDays;
    private String cleanupCycle;
    private boolean enabled;

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getCleanupCycle() {
        return cleanupCycle;
    }

    public void setCleanupCycle(String cleanupCycle) {
        this.cleanupCycle = cleanupCycle;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
