package com.example.userservice.controller;

import com.example.userservice.common.ApiResponse;
import com.example.userservice.common.BusinessException;
import com.example.userservice.service.UserAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/groups")
@Tag(name = "Admin - Groups")
public class AdminGroupController {
    private final UserAdminService userAdminService;

    public AdminGroupController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listGroups(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return ApiResponse.ok(userAdminService.listGroups(keyword, page, pageSize));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createGroup(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(userAdminService.createGroup(
                required(body, "name", "group_name", "groupName"),
                RequestMaps.stringValue(body, "description")));
    }

    @PutMapping("/{groupId}")
    public ApiResponse<Map<String, Object>> updateGroup(@PathVariable Long groupId,
                                                        @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(userAdminService.updateGroup(
                groupId,
                RequestMaps.stringValue(body, "name", "group_name", "groupName"),
                RequestMaps.stringValue(body, "description")));
    }

    @DeleteMapping("/{groupId}")
    public ApiResponse<Map<String, Object>> deleteGroup(@PathVariable Long groupId) {
        userAdminService.deleteGroup(groupId);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    @PostMapping("/{groupId}/users")
    public ApiResponse<Map<String, Object>> addUser(@PathVariable Long groupId,
                                                    @RequestBody Map<String, Object> body) {
        userAdminService.addUserToGroup(groupId, requiredLong(body, "user_id", "userId"));
        return ApiResponse.ok(Map.of("added", true));
    }

    @DeleteMapping("/{groupId}/users/{userId}")
    public ApiResponse<Map<String, Object>> removeUser(@PathVariable Long groupId,
                                                       @PathVariable Long userId) {
        userAdminService.removeUserFromGroup(groupId, userId);
        return ApiResponse.ok(Map.of("removed", true));
    }

    @GetMapping("/{groupId}/users")
    public ApiResponse<Map<String, Object>> users(@PathVariable Long groupId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return ApiResponse.ok(userAdminService.groupUsers(groupId, page, pageSize));
    }

    private String required(Map<String, Object> body, String... keys) {
        String value = RequestMaps.stringValue(body, keys);
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, "required field missing: " + String.join("/", keys));
        }
        return value;
    }

    private Long requiredLong(Map<String, Object> body, String... keys) {
        Long value = RequestMaps.longValue(body, keys);
        if (value == null) {
            throw new BusinessException(400, "required field missing: " + String.join("/", keys));
        }
        return value;
    }
}
