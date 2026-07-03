package com.example.logservice.controller;

import com.example.logservice.common.ApiResponse;
import com.example.logservice.dto.AccessLogSearchRequest;
import com.example.logservice.model.AccessLog;
import com.example.logservice.service.LogQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/log")
public class LogController {
    private static final Logger log = LoggerFactory.getLogger(LogController.class);

    private final LogQueryService logQueryService;

    public LogController(LogQueryService logQueryService) {
        this.logQueryService = logQueryService;
    }

    @PostMapping("/search")
    public ApiResponse<Map<String, Object>> search(@RequestBody(required = false) AccessLogSearchRequest request) {
        AccessLogSearchRequest safeRequest = request == null ? new AccessLogSearchRequest() : request;
        List<AccessLog> matched = logQueryService.search(safeRequest);
        log.info("search access logs: serviceName={}, path={}, statusCode={}, traceId={}, total={}",
                safeRequest.getServiceName(), safeRequest.getPath(), safeRequest.getStatusCode(),
                safeRequest.getTraceId(), matched.size());
        return ApiResponse.ok(Map.of(
                "total", matched.size(),
                "page", safeRequest.getPage() == null ? 1 : safeRequest.getPage(),
                "data", logQueryService.page(matched, safeRequest.getPage(), safeRequest.getPageSize())
        ));
    }
}
