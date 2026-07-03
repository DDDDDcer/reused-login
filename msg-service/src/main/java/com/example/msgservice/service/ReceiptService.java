package com.example.msgservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.msgservice.entity.MessageLog;
import com.example.msgservice.entity.MessageTask;
import com.example.msgservice.mapper.MessageLogMapper;
import com.example.msgservice.mapper.MessageTaskMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReceiptService {
    private final MessageTaskMapper taskMapper;
    private final MessageLogMapper logMapper;
    private final ObjectMapper objectMapper;

    public ReceiptService(MessageTaskMapper taskMapper, MessageLogMapper logMapper, ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.logMapper = logMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> process(Map<String, Object> body) {
        Long taskId = longValue(body.getOrDefault("task_id", body.get("taskId")));
        String platform = stringValue(body.getOrDefault("platform", "UNKNOWN"));
        String receiptStatus = normalizeStatus(stringValue(body.getOrDefault("receipt_status",
                body.getOrDefault("status", "RECEIVED"))));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", true);
        result.put("platform", platform);
        result.put("receipt_status", receiptStatus);

        if (taskId == null) {
            result.put("matched", false);
            result.put("note", "receipt accepted without task_id");
            return result;
        }

        MessageTask task = taskMapper.selectById(taskId);
        if (task == null) {
            result.put("matched", false);
            result.put("task_id", taskId);
            result.put("note", "message task not found");
            return result;
        }

        String taskStatus = toTaskStatus(receiptStatus);
        task.setStatus(taskStatus);
        if ("SUCCESS".equals(taskStatus)) {
            task.setSentAt(LocalDateTime.now());
            task.setFailReason(null);
        } else if ("FINAL_FAILED".equals(taskStatus)) {
            task.setFailReason(stringValue(body.getOrDefault("fail_reason", body.get("failReason"))));
        }
        taskMapper.updateById(task);

        MessageLog log = new MessageLog();
        log.setTaskId(taskId);
        log.setAttemptNo(nextAttemptNo(taskId));
        log.setStatus(receiptStatus);
        log.setProviderResponse(toJson(body));
        log.setFailReason(task.getFailReason());
        log.setExecutedAt(LocalDateTime.now());
        logMapper.insert(log);

        result.put("matched", true);
        result.put("task_id", taskId);
        result.put("task_status", taskStatus);
        result.put("log_id", log.getId());
        return result;
    }

    private int nextAttemptNo(Long taskId) {
        Long count = logMapper.selectCount(new LambdaQueryWrapper<MessageLog>().eq(MessageLog::getTaskId, taskId));
        return count.intValue() + 1;
    }

    private String toTaskStatus(String receiptStatus) {
        return switch (receiptStatus) {
            case "DELIVERED", "SUCCESS" -> "SUCCESS";
            case "FAILED", "BOUNCED", "REJECTED" -> "FINAL_FAILED";
            default -> "SUCCESS";
        };
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "RECEIVED";
        String normalized = status.toUpperCase();
        return normalized.length() <= 16 ? normalized : normalized.substring(0, 16);
    }

    private Long longValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        return Long.parseLong(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String toJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body == null ? Map.of() : body);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
