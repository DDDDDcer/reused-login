package com.example.userservice.controller;

import com.example.userservice.common.ApiResponse;
import com.example.userservice.common.BusinessException;
import com.example.userservice.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin - 用户管理", description = "管理员用户管理接口（需 admin 角色）")
public class AdminUserController {
    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    @Operation(summary = "查询用户列表", description = "分页查询用户列表，支持关键词和状态筛选")
    public ApiResponse<Map<String, Object>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return ApiResponse.ok(userAdminService.listUsers(keyword, status, page, pageSize));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "查询用户详情", description = "根据用户ID查询用户详情")
    public ApiResponse<Map<String, Object>> getUser(@PathVariable Long userId) {
        return ApiResponse.ok(userAdminService.getUserDetail(userId));
    }

    @PostMapping
    @Operation(summary = "创建用户", description = "后台新增用户")
    public ApiResponse<Map<String, Object>> createUser(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(userAdminService.createUser(
                required(body, "username", "account"),
                RequestMaps.stringValue(body, "email"),
                RequestMaps.stringValue(body, "mobile"),
                RequestMaps.stringValue(body, "password"),
                RequestMaps.mapValue(body, "attributes")
        ));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "修改用户", description = "修改指定用户信息")
    public ApiResponse<Map<String, Object>> updateUser(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(userAdminService.updateUser(
                userId,
                RequestMaps.stringValue(body, "username", "account"),
                RequestMaps.stringValue(body, "email"),
                RequestMaps.stringValue(body, "mobile"),
                RequestMaps.mapValue(body, "attributes")
        ));
    }

    @PostMapping("/{userId}/enable")
    @Operation(summary = "启用用户", description = "启用指定用户")
    public ApiResponse<Map<String, Object>> enableUser(@PathVariable Long userId) {
        userAdminService.enableUser(userId);
        return ApiResponse.ok(Map.of("enabled", true));
    }

    @PostMapping("/{userId}/disable")
    @Operation(summary = "停用用户", description = "停用指定用户")
    public ApiResponse<Map<String, Object>> disableUser(@PathVariable Long userId) {
        userAdminService.disableUser(userId);
        return ApiResponse.ok(Map.of("disabled", true));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "删除用户", description = "删除指定用户（级联删除凭证、属性、第三方绑定）")
    public ApiResponse<Map<String, Object>> deleteUser(@PathVariable Long userId) {
        userAdminService.deleteUser(userId);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    @PostMapping("/{userId}/password/reset")
    @Operation(summary = "管理员重置密码", description = "管理员为指定用户重置密码")
    public ApiResponse<Map<String, Object>> resetPassword(@PathVariable Long userId, @RequestBody(required = false) Map<String, Object> body) {
        String newPassword = body == null ? null : RequestMaps.stringValue(body, "new_password", "newPassword", "password");
        userAdminService.resetPassword(userId, newPassword);
        return ApiResponse.ok(Map.of("reset", true, "default_password", newPassword == null ? "123456" : newPassword));
    }

    private String required(Map<String, Object> body, String... keys) {
        String value = RequestMaps.stringValue(body, keys);
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, "required field missing: " + String.join("/", keys));
        }
        return value;
    }
}