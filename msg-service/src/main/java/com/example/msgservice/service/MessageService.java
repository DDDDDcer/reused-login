package com.example.msgservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.msgservice.common.BusinessException;
import com.example.msgservice.entity.Message;
import com.example.msgservice.entity.MessageLog;
import com.example.msgservice.entity.MessageTask;
import com.example.msgservice.mapper.MessageLogMapper;
import com.example.msgservice.mapper.MessageMapper;
import com.example.msgservice.mapper.MessageTaskMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageService {
    private static final int MAX_RETRIES = 3;
    private static final int[] RETRY_BACKOFF_SECONDS = {60, 300, 900};

    private final MessageMapper messageMapper;
    private final MessageTaskMapper taskMapper;
    private final MessageLogMapper logMapper;
    private final TemplateService templateService;
    private final CarrierService carrierService;
    private final ObjectMapper objectMapper;

    public MessageService(MessageMapper messageMapper, MessageTaskMapper taskMapper, MessageLogMapper logMapper,
                          TemplateService templateService, CarrierService carrierService, ObjectMapper objectMapper) {
        this.messageMapper = messageMapper;
        this.taskMapper = taskMapper;
        this.logMapper = logMapper;
        this.templateService = templateService;
        this.carrierService = carrierService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> sendNow(String senderId, String bizCode, String sceneCode, String receiverId,
                                       String carrierType, Long templateId, String title, String content, Map<String, Object> variables,
                                       String idempotencyKey) {
        String requestHash = requestHash(receiverId, carrierType, templateId, title, content, variables);
        MessageTask existing = findByIdempotencyKey(bizCode, sceneCode, idempotencyKey);
        if (existing != null) return idempotentSummary(existing, requestHash);
        MessageTask task = createTask(senderId, receiverId, carrierType, templateId, title, content, variables,
                normalizeBizCode(bizCode), normalizeSceneCode(sceneCode), idempotencyKey, requestHash,
                LocalDateTime.now(), "WAITING");
        appendLog(task.getId(), task.getRetryCount() + 1, "WAITING", "task ready for immediate sending", null);
        claimAndExecute(task.getId());
        return taskSummary(requireTask(task.getId()));
    }

    @Transactional
    public Map<String, Object> schedule(String senderId, String bizCode, String sceneCode, String receiverId,
                                        String carrierType, Long templateId, String title, String content, Map<String, Object> variables,
                                        String idempotencyKey, LocalDateTime plannedTime) {
        if (plannedTime == null || !plannedTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException(400, "planned_time must be later than current time");
        }
        String requestHash = requestHash(receiverId, carrierType, templateId, title, content, variables);
        MessageTask existing = findByIdempotencyKey(bizCode, sceneCode, idempotencyKey);
        if (existing != null) return idempotentSummary(existing, requestHash);
        MessageTask task = createTask(senderId, receiverId, carrierType, templateId, title, content, variables,
                normalizeBizCode(bizCode), normalizeSceneCode(sceneCode), idempotencyKey, requestHash,
                plannedTime, "SCHEDULED");
        appendLog(task.getId(), 0, "SCHEDULED", "scheduled task created", null);
        return taskSummary(task);
    }

    @Scheduled(fixedDelay = 5000)
    public void dispatchDueTasks() {
        List<MessageTask> due = taskMapper.selectList(new LambdaQueryWrapper<MessageTask>()
                .eq(MessageTask::getStatus, "SCHEDULED")
                .le(MessageTask::getPlannedTime, LocalDateTime.now())
                .orderByAsc(MessageTask::getPlannedTime)
                .last("LIMIT 100"));
        due.forEach(task -> {
            int released = taskMapper.update(null, new LambdaUpdateWrapper<MessageTask>()
                    .eq(MessageTask::getId, task.getId())
                    .eq(MessageTask::getStatus, "SCHEDULED")
                    .set(MessageTask::getStatus, "WAITING"));
            if (released == 1) {
                appendLog(task.getId(), task.getRetryCount() + 1, "WAITING", "planned_time reached", null);
                claimAndExecute(task.getId());
            }
        });

        List<MessageTask> retryable = taskMapper.selectList(new LambdaQueryWrapper<MessageTask>()
                .eq(MessageTask::getStatus, "RETRY_WAITING")
                .le(MessageTask::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(MessageTask::getNextRetryTime)
                .last("LIMIT 100"));
        retryable.forEach(task -> {
            int released = taskMapper.update(null, new LambdaUpdateWrapper<MessageTask>()
                    .eq(MessageTask::getId, task.getId())
                    .eq(MessageTask::getStatus, "RETRY_WAITING")
                    .eq(MessageTask::getRetryCount, task.getRetryCount())
                    .set(MessageTask::getStatus, "WAITING")
                    .set(MessageTask::getNextRetryTime, null));
            if (released == 1) claimAndExecute(task.getId());
        });
    }

    public Map<String, Object> taskDetail(Long taskId) {
        MessageTask task = requireTask(taskId);
        Message message = messageMapper.selectById(task.getMessageId());
        Map<String, Object> result = taskSummary(task);
        result.put("message", message);
        result.put("logs", logMapper.selectList(new LambdaQueryWrapper<MessageLog>()
                .eq(MessageLog::getTaskId, taskId).orderByAsc(MessageLog::getAttemptNo)));
        return result;
    }

    public Map<String, Object> records(String senderId, String status, String carrierType, String receiverId,
                                       LocalDateTime startTime, LocalDateTime endTime, int page, int pageSize) {
        LambdaQueryWrapper<MessageTask> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) wrapper.eq(MessageTask::getStatus, status.toUpperCase());
        if (carrierType != null && !carrierType.isBlank()) wrapper.eq(MessageTask::getCarrierType, carrierType.toUpperCase());
        if (receiverId != null && !receiverId.isBlank()) wrapper.eq(MessageTask::getReceiverId, receiverId);
        if (startTime != null) wrapper.ge(MessageTask::getCreatedAt, startTime);
        if (endTime != null) wrapper.le(MessageTask::getCreatedAt, endTime);
        wrapper.orderByDesc(MessageTask::getCreatedAt);

        List<Map<String, Object>> visible = taskMapper.selectList(wrapper).stream()
                .filter(task -> {
                    Message message = messageMapper.selectById(task.getMessageId());
                    return senderId == null || senderId.isBlank() || senderId.equals(message.getSenderId());
                })
                .map(this::taskSummary)
                .toList();
        int from = Math.min(Math.max(0, (page - 1) * pageSize), visible.size());
        int to = Math.min(from + pageSize, visible.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_list", visible.subList(from, to));
        result.put("total", visible.size());
        result.put("page", page);
        result.put("page_size", pageSize);
        return result;
    }

    public Map<String, Object> localMessages(String userId, String status, LocalDateTime startTime,
                                              LocalDateTime endTime, int page, int pageSize) {
        LambdaQueryWrapper<MessageTask> wrapper = new LambdaQueryWrapper<MessageTask>()
                .eq(MessageTask::getCarrierType, "LOCAL")
                .eq(MessageTask::getReceiverId, userId)
                .eq(MessageTask::getLocalDeleted, 0);
        if (status != null && !status.isBlank()) wrapper.eq(MessageTask::getStatus, status.toUpperCase());
        if (startTime != null) wrapper.ge(MessageTask::getCreatedAt, startTime);
        if (endTime != null) wrapper.le(MessageTask::getCreatedAt, endTime);
        List<MessageTask> tasks = taskMapper.selectList(wrapper.orderByDesc(MessageTask::getCreatedAt));
        int from = Math.min(Math.max(0, (page - 1) * pageSize), tasks.size());
        int to = Math.min(from + pageSize, tasks.size());
        List<Map<String, Object>> items = new ArrayList<>();
        for (MessageTask task : tasks.subList(from, to)) {
            Message message = messageMapper.selectById(task.getMessageId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("message_id", message.getId());
            item.put("title", message.getTitle());
            item.put("content_summary", summarize(message.getContent()));
            item.put("sender_id", message.getSenderId());
            item.put("sent_at", task.getSentAt());
            item.put("status", task.getStatus());
            items.add(item);
        }
        return Map.of("message_list", items, "total", tasks.size(), "page", page, "page_size", pageSize);
    }

    public Map<String, Object> localDetail(String userId, Long messageId) {
        MessageTask task = requireLocalTask(userId, messageId);
        Message message = messageMapper.selectById(messageId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message_id", messageId);
        result.put("title", message.getTitle());
        result.put("content", message.getContent());
        result.put("sender_id", message.getSenderId());
        result.put("created_at", message.getCreatedAt());
        result.put("status", task.getStatus());
        return result;
    }

    public Map<String, Object> deleteLocal(String userId, Long messageId) {
        MessageTask task = requireLocalTask(userId, messageId);
        task.setLocalDeleted(1);
        taskMapper.updateById(task);
        return Map.of("deleted", true);
    }

    private MessageTask createTask(String senderId, String receiverId, String carrierType, Long templateId,
                                   String title, String content, Map<String, Object> variables,
                                   String bizCode, String sceneCode, String idempotencyKey, String requestHash,
                                   LocalDateTime plannedTime, String status) {
        if (receiverId == null || receiverId.isBlank()) throw new BusinessException(400, "receiver_id is required");
        String normalizedCarrier = carrierType.toUpperCase();
        CarrierService.CarrierSelection selection = carrierService.selectEnabled(normalizedCarrier);
        String renderedContent = templateId == null ? content : templateService.render(templateId, variables, normalizedCarrier);
        if (renderedContent == null || renderedContent.isBlank()) {
            throw new BusinessException(400, "content or template_id is required");
        }

        Message message = new Message();
        message.setTemplateId(templateId);
        message.setTitle(title);
        message.setContent(renderedContent);
        message.setVariablesJson(toJson(variables));
        message.setSenderId(senderId == null || senderId.isBlank() ? "system" : senderId);
        message.setReceiverId(receiverId);
        messageMapper.insert(message);

        MessageTask task = new MessageTask();
        task.setMessageId(message.getId());
        task.setCarrierId(selection.carrier().getId());
        task.setAccountId(selection.account().getId());
        task.setCarrierType(normalizedCarrier);
        task.setReceiverId(receiverId);
        task.setStatus(status);
        task.setBizCode(bizCode);
        task.setSceneCode(sceneCode);
        task.setIdempotencyKey(idempotencyKey);
        task.setRequestHash(requestHash);
        task.setRetryCount(0);
        task.setLocalDeleted(0);
        task.setPlannedTime(plannedTime);
        taskMapper.insert(task);
        appendLog(task.getId(), 0, "CREATED", "message task created", null);
        return task;
    }

    private void claimAndExecute(Long taskId) {
        int claimed = taskMapper.update(null, new LambdaUpdateWrapper<MessageTask>()
                .eq(MessageTask::getId, taskId)
                .eq(MessageTask::getStatus, "WAITING")
                .set(MessageTask::getStatus, "SENDING"));
        if (claimed == 0) return;

        MessageTask task = requireTask(taskId);
        appendLog(taskId, task.getRetryCount() + 1, "SENDING", "executor claimed task", null);
        MessageLog log = new MessageLog();
        log.setTaskId(taskId);
        log.setAttemptNo(task.getRetryCount() + 1);
        log.setExecutedAt(LocalDateTime.now());
        try {
            // Carrier calls are intentionally mocked. Replace this branch with provider adapters.
            if (task.getReceiverId().startsWith("FAIL_")) {
                throw new IllegalStateException("mock provider rejected receiver");
            }
            task.setStatus("SUCCESS");
            task.setSentAt(LocalDateTime.now());
            task.setFailReason(null);
            task.setNextRetryTime(null);
            log.setStatus("SUCCESS");
            log.setProviderResponse("mock provider accepted");
        } catch (Exception ex) {
            task.setFailReason(ex.getMessage());
            task.setRetryCount(task.getRetryCount() + 1);
            if (task.getRetryCount() >= MAX_RETRIES || isNonRetryable(ex)) {
                task.setStatus("FINAL_FAILED");
                task.setNextRetryTime(null);
            } else {
                task.setStatus("RETRY_WAITING");
                task.setNextRetryTime(LocalDateTime.now().plusSeconds(backoffSeconds(task.getRetryCount())));
            }
            log.setStatus(task.getStatus());
            log.setFailReason(ex.getMessage());
        }
        taskMapper.updateById(task);
        logMapper.insert(log);
    }

    private MessageTask findByIdempotencyKey(String bizCode, String sceneCode, String key) {
        if (key == null || key.isBlank()) return null;
        return taskMapper.selectOne(new LambdaQueryWrapper<MessageTask>()
                .eq(MessageTask::getBizCode, normalizeBizCode(bizCode))
                .eq(MessageTask::getSceneCode, normalizeSceneCode(sceneCode))
                .eq(MessageTask::getIdempotencyKey, key).last("LIMIT 1"));
    }

    private MessageTask requireTask(Long taskId) {
        MessageTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "message task not found: " + taskId);
        return task;
    }

    private MessageTask requireLocalTask(String userId, Long messageId) {
        MessageTask task = taskMapper.selectOne(new LambdaQueryWrapper<MessageTask>()
                .eq(MessageTask::getMessageId, messageId)
                .eq(MessageTask::getCarrierType, "LOCAL")
                .eq(MessageTask::getReceiverId, userId)
                .eq(MessageTask::getLocalDeleted, 0)
                .last("LIMIT 1"));
        if (task == null) throw new BusinessException(404, "local message not found or not visible");
        return task;
    }

    private Map<String, Object> taskSummary(MessageTask task) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_id", task.getId());
        result.put("message_id", task.getMessageId());
        result.put("task_status", task.getStatus());
        result.put("send_status", task.getStatus());
        result.put("biz_code", task.getBizCode());
        result.put("scene_code", task.getSceneCode());
        result.put("idempotency_key", task.getIdempotencyKey());
        result.put("carrier_type", task.getCarrierType());
        result.put("receiver_id", task.getReceiverId());
        result.put("planned_time", task.getPlannedTime());
        result.put("next_retry_time", task.getNextRetryTime());
        result.put("sent_at", task.getSentAt());
        result.put("fail_reason", task.getFailReason());
        result.put("retry_count", task.getRetryCount());
        return result;
    }

    private Map<String, Object> idempotentSummary(MessageTask existing, String requestHash) {
        if (existing.getRequestHash() != null && !existing.getRequestHash().equals(requestHash)) {
            throw new BusinessException(409, "IDEMPOTENCY_CONFLICT");
        }
        return taskSummary(existing);
    }

    private void appendLog(Long taskId, int attemptNo, String status, String providerResponse, String failReason) {
        MessageLog log = new MessageLog();
        log.setTaskId(taskId);
        log.setAttemptNo(attemptNo);
        log.setStatus(status);
        log.setProviderResponse(providerResponse);
        log.setFailReason(failReason);
        log.setExecutedAt(LocalDateTime.now());
        logMapper.insert(log);
    }

    private String normalizeBizCode(String bizCode) {
        return bizCode == null || bizCode.isBlank() ? "topbiz" : bizCode;
    }

    private String normalizeSceneCode(String sceneCode) {
        return sceneCode == null || sceneCode.isBlank() ? "default" : sceneCode;
    }

    private String requestHash(String receiverId, String carrierType, Long templateId, String title,
                               String content, Map<String, Object> variables) {
        return Integer.toHexString(toJson(Map.of(
                "receiver_id", receiverId,
                "carrier_type", carrierType,
                "template_id", templateId == null ? "" : templateId,
                "title", title == null ? "" : title,
                "content", content == null ? "" : content,
                "variables", variables == null ? Map.of() : variables
        )).hashCode());
    }

    private boolean isNonRetryable(Exception ex) {
        return ex instanceof BusinessException;
    }

    private int backoffSeconds(int retryCount) {
        int index = Math.min(Math.max(retryCount - 1, 0), RETRY_BACKOFF_SECONDS.length - 1);
        return RETRY_BACKOFF_SECONDS[index];
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(400, "invalid variables");
        }
    }

    private String summarize(String content) {
        return content.length() <= 100 ? content : content.substring(0, 100);
    }
}
