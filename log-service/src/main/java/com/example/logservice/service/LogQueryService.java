package com.example.logservice.service;

import com.example.logservice.dto.AccessLogSearchRequest;
import com.example.logservice.model.AccessLog;
import com.example.logservice.store.InMemoryLogStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
public class LogQueryService {
    private final InMemoryLogStore store;

    public LogQueryService(InMemoryLogStore store) {
        this.store = store;
    }

    public List<AccessLog> search(AccessLogSearchRequest request) {
        return store.getAccessLogs().stream()
                .filter(log -> request.getStartTime() == null || !log.getRequestTime().isBefore(request.getStartTime()))
                .filter(log -> request.getEndTime() == null || !log.getRequestTime().isAfter(request.getEndTime()))
                .filter(log -> matches(request.getUserId(), log.getUserId()))
                .filter(log -> matches(request.getClientIp(), log.getClientIp()))
                .filter(log -> matches(request.getServiceName(), log.getServiceName()))
                .filter(log -> matches(request.getPath(), log.getPath()))
                .filter(log -> request.getStatusCode() == null || request.getStatusCode() == log.getStatusCode())
                .filter(log -> matches(request.getTraceId(), log.getTraceId()))
                .sorted(Comparator.comparing(AccessLog::getRequestTime).reversed())
                .toList();
    }

    public <T> List<T> page(List<T> items, Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 200);
        int from = Math.min((safePage - 1) * safePageSize, items.size());
        int to = Math.min(from + safePageSize, items.size());
        return items.subList(from, to);
    }

    private boolean matches(String expected, String actual) {
        return !StringUtils.hasText(expected) || (actual != null && actual.contains(expected));
    }
}
