package com.example.logservice.model;

public class CollectionConfig {
    private String logPath;
    private String format;
    private boolean enabled;
    private int batchSize;
    private String flushInterval;
    private String targetStorage;

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public String getFlushInterval() {
        return flushInterval;
    }

    public void setFlushInterval(String flushInterval) {
        this.flushInterval = flushInterval;
    }

    public String getTargetStorage() {
        return targetStorage;
    }

    public void setTargetStorage(String targetStorage) {
        this.targetStorage = targetStorage;
    }
}
