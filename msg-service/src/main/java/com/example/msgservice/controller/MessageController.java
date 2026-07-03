package com.example.msgservice.controller;

import com.example.msgservice.common.ApiResponse;
import com.example.msgservice.service.MessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "Messages")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/send-now")
    public ApiResponse<Map<String, Object>> sendNow(
            @RequestHeader(name = "X-Sender-Id", required = false) String senderId,
            @RequestHeader(name = "Idempotency-Key", required = false) String headerKey,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(messageService.sendNow(
                senderId,
                RequestMaps.stringValue(body, "biz_code", "bizCode"),
                RequestMaps.stringValue(body, "scene_code", "sceneCode"),
                RequestMaps.required(body, "receiver_id", "receiverId"),
                RequestMaps.required(body, "carrier_type", "carrierType"),
                RequestMaps.longValue(body, "template_id", "templateId"),
                RequestMaps.stringValue(body, "title"),
                RequestMaps.stringValue(body, "content"),
                RequestMaps.mapValue(body, "variables"),
                headerKey == null ? RequestMaps.stringValue(body, "idempotency_key") : headerKey));
    }

    @PostMapping("/send-scheduled")
    public ApiResponse<Map<String, Object>> sendScheduled(
            @RequestHeader(name = "X-Sender-Id", required = false) String senderId,
            @RequestHeader(name = "Idempotency-Key", required = false) String headerKey,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(messageService.schedule(
                senderId,
                RequestMaps.stringValue(body, "biz_code", "bizCode"),
                RequestMaps.stringValue(body, "scene_code", "sceneCode"),
                RequestMaps.required(body, "receiver_id", "receiverId"),
                RequestMaps.required(body, "carrier_type", "carrierType"),
                RequestMaps.longValue(body, "template_id", "templateId"),
                RequestMaps.stringValue(body, "title"),
                RequestMaps.stringValue(body, "content"),
                RequestMaps.mapValue(body, "variables"),
                headerKey == null ? RequestMaps.stringValue(body, "idempotency_key") : headerKey,
                RequestMaps.dateTime(RequestMaps.required(body, "planned_time", "plannedTime"))));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<Map<String, Object>> taskDetail(@PathVariable Long taskId) {
        return ApiResponse.ok(messageService.taskDetail(taskId));
    }

    @GetMapping("/records")
    public ApiResponse<Map<String, Object>> records(
            @RequestHeader(name = "X-Sender-Id", required = false) String senderId,
            @RequestParam(required = false) String status,
            @RequestParam(name = "carrier_type", required = false) String carrierType,
            @RequestParam(name = "receiver_id", required = false) String receiverId,
            @RequestParam(name = "start_time", required = false) String startTime,
            @RequestParam(name = "end_time", required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return ApiResponse.ok(messageService.records(senderId, status, carrierType, receiverId,
                RequestMaps.dateTime(startTime), RequestMaps.dateTime(endTime), page, pageSize));
    }

    @GetMapping("/local")
    public ApiResponse<Map<String, Object>> localMessages(
            @RequestHeader(name = "X-User-Id", defaultValue = "1") String userId,
            @RequestParam(required = false) String status,
            @RequestParam(name = "start_time", required = false) String startTime,
            @RequestParam(name = "end_time", required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return ApiResponse.ok(messageService.localMessages(userId, status, RequestMaps.dateTime(startTime),
                RequestMaps.dateTime(endTime), page, pageSize));
    }

    @GetMapping("/local/{messageId}")
    public ApiResponse<Map<String, Object>> localDetail(
            @RequestHeader(name = "X-User-Id", defaultValue = "1") String userId,
            @PathVariable Long messageId) {
        return ApiResponse.ok(messageService.localDetail(userId, messageId));
    }

    @DeleteMapping("/local/{messageId}")
    public ApiResponse<Map<String, Object>> deleteLocal(
            @RequestHeader(name = "X-User-Id", defaultValue = "1") String userId,
            @PathVariable Long messageId) {
        return ApiResponse.ok(messageService.deleteLocal(userId, messageId));
    }
}
