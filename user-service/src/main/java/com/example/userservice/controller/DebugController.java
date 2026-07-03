package com.example.userservice.controller;

import com.example.userservice.common.ApiResponse;
import com.example.userservice.service.UserAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/debug")
public class DebugController {
    private final UserAdminService userAdminService;

    public DebugController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping("/state")
    public ApiResponse<Map<String, Object>> state() {
        return ApiResponse.ok(userAdminService.debugState());
    }

    @PostMapping("/reset")
    public ApiResponse<Map<String, Object>> reset() {
        userAdminService.reset();
        return ApiResponse.ok(Map.of("reset", true));
    }
}