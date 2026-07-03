package com.example.logservice.service;

import com.example.logservice.dto.RetentionPolicyRequest;
import com.example.logservice.model.RetentionPolicy;
import com.example.logservice.store.InMemoryLogStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ConfigService {
    private final InMemoryLogStore store;

    public ConfigService(InMemoryLogStore store) {
        this.store = store;
    }

    public RetentionPolicy updateRetention(RetentionPolicyRequest request) {
        RetentionPolicy policy = store.getRetentionPolicy();
        if (request.getRetentionDays() != null) {
            policy.setRetentionDays(request.getRetentionDays());
        }
        if (request.getCleanupCycle() != null) {
            policy.setCleanupCycle(request.getCleanupCycle());
        }
        if (request.getEnabled() != null) {
            policy.setEnabled(request.getEnabled());
        }
        store.setRetentionPolicy(policy);
        return policy;
    }

    public int cleanup() {
        RetentionPolicy policy = store.getRetentionPolicy();
        if (!policy.isEnabled()) {
            return 0;
        }
        return store.removeAccessLogsBefore(LocalDateTime.now().minusDays(policy.getRetentionDays()));
    }
}
