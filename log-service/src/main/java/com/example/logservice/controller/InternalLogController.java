package com.example.logservice.controller;

import com.example.logservice.common.ApiResponse;
import com.example.logservice.dto.AccessLogReportRequest;
import com.example.logservice.model.AccessLog;
import com.example.logservice.service.InternalLogService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/logs")
public class InternalLogController {
    private final InternalLogService internalLogService;

    public InternalLogController(InternalLogService internalLogService) {
        this.internalLogService = internalLogService;
    }

    @PostMapping("/access")
    public ApiResponse<Map<String, Object>> report(@RequestBody AccessLogReportRequest request) {
        AccessLog log = internalLogService.report(request);
        return ApiResponse.ok(Map.of("report_result", true, "log", log));
    }
}
