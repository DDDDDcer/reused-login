package com.example.logservice.service;

import com.example.logservice.dto.AccessLogReportRequest;
import com.example.logservice.model.AccessLog;
import com.example.logservice.store.InMemoryLogStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InternalLogService {
    private final InMemoryLogStore store;
    private final AlertService alertService;

    public InternalLogService(InMemoryLogStore store, AlertService alertService) {
        this.store = store;
        this.alertService = alertService;
    }

    public AccessLog report(AccessLogReportRequest request) {
        AccessLog log = new AccessLog();
        int statusCode = request.getStatusCode() == null ? 200 : request.getStatusCode();
        log.setLogId(defaultText(request.getLogId(), "log-" + UUID.randomUUID()));
        log.setRequestTime(request.getRequestTime() == null ? LocalDateTime.now() : request.getRequestTime());
        log.setUserId(defaultText(request.getUserId(), "anonymous"));
        log.setClientIp(defaultText(request.getClientIp(), "0.0.0.0"));
        log.setServiceName(defaultText(request.getServiceName(), "topbiz"));
        log.setPath(defaultText(request.getPath(), "/"));
        log.setMethod(defaultText(request.getMethod(), "GET"));
        log.setStatusCode(statusCode);
        log.setCostMs(request.getCostMs() == null ? 0 : request.getCostMs());
        log.setTraceId(defaultText(request.getTraceId(), "trace-" + UUID.randomUUID()));
        log.setLevel(statusCode >= 500 ? "ERROR" : statusCode >= 400 ? "WARN" : "INFO");
        log.setMessage(defaultText(request.getMessage(), "reported by TopBiz"));
        store.addAccessLog(log);
        alertService.checkRules(log);
        return log;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
