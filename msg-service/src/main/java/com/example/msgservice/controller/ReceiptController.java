package com.example.msgservice.controller;

import com.example.msgservice.common.ApiResponse;
import com.example.msgservice.service.ReceiptService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/message-receipts")
@Tag(name = "Message Receipts")
public class ReceiptController {
    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> process(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(receiptService.process(body));
    }
}
