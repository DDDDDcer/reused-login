package com.example.userservice.controller;

import com.example.userservice.common.ApiResponse;
import com.example.userservice.common.BusinessException;
import com.example.userservice.service.UserAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Admin - Permissions")
public class AdminPermissionController {
    private final UserAdminService userAdminService;

    public AdminPermissionController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping("/api/v1/admin/permissions")
    public ApiResponse<Map<String, Object>> listPermissions() {
        return ApiResponse.ok(userAdminService.listPermissions());
    }

    @PostMapping("/api/v1/admin/permissions")
    public ApiResponse<Map<String, Object>> createPermission(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(userAdminService.createPermission(
                required(body, "code", "permission_code", "permissionCode"),
                required(body, "name", "permission_name", "permissionName"),
                RequestMaps.stringValue(body, "resource_type", "resourceType"),
                RequestMaps.stringValue(body, "description")));
    }

    @DeleteMapping("/api/v1/admin/permissions/{permissionId}")
    public ApiResponse<Map<String, Object>> deletePermission(@PathVariable Long permissionId) {
        userAdminService.deletePermission(permissionId);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    @GetMapping("/api/v1/admin/groups/{groupId}/permissions")
    public ApiResponse<Map<String, Object>> groupPermissions(@PathVariable Long groupId) {
        return ApiResponse.ok(userAdminService.groupPermissions(groupId));
    }

    @PostMapping("/api/v1/admin/groups/{groupId}/permissions")
    public ApiResponse<Map<String, Object>> grantPermissions(@PathVariable Long groupId,
                                                             @RequestBody Map<String, Object> body) {
        userAdminService.grantPermissions(groupId, permissionIds(body));
        return ApiResponse.ok(Map.of("granted", true));
    }

    @DeleteMapping("/api/v1/admin/groups/{groupId}/permissions/{permissionId}")
    public ApiResponse<Map<String, Object>> revokePermission(@PathVariable Long groupId,
                                                             @PathVariable Long permissionId) {
        userAdminService.revokePermission(groupId, permissionId);
        return ApiResponse.ok(Map.of("revoked", true));
    }

    private List<Long> permissionIds(Map<String, Object> body) {
        Object raw = body.get("permission_ids");
        if (raw == null) raw = body.get("permissionIds");
        List<Long> ids = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) ids.add(Long.parseLong(String.valueOf(item)));
        } else {
            Long id = RequestMaps.longValue(body, "permission_id", "permissionId");
            if (id != null) ids.add(id);
        }
        if (ids.isEmpty()) throw new BusinessException(400, "permission_ids is required");
        return ids;
    }

    private String required(Map<String, Object> body, String... keys) {
        String value = RequestMaps.stringValue(body, keys);
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, "required field missing: " + String.join("/", keys));
        }
        return value;
    }
}
