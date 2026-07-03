package com.example.userservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.userservice.common.BusinessException;
import com.example.userservice.entity.*;
import com.example.userservice.mapper.*;
import com.example.userservice.shiro.UserRealm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserAdminService {

    private final UserMapper userMapper;
    private final CredentialMapper credentialMapper;
    private final UserAttributeValueMapper userAttributeValueMapper;
    private final ThirdPartyBindingMapper thirdPartyBindingMapper;
    private final VerificationCodeMapper verificationCodeMapper;
    private final GroupMapper groupMapper;
    private final UserGroupMapper userGroupMapper;
    private final PermissionMapper permissionMapper;
    private final GroupPermissionMapper groupPermissionMapper;
    private final AuthService authService;

    public UserAdminService(UserMapper userMapper,
                            CredentialMapper credentialMapper,
                            UserAttributeValueMapper userAttributeValueMapper,
                            ThirdPartyBindingMapper thirdPartyBindingMapper,
                            VerificationCodeMapper verificationCodeMapper,
                            GroupMapper groupMapper,
                            UserGroupMapper userGroupMapper,
                            PermissionMapper permissionMapper,
                            GroupPermissionMapper groupPermissionMapper,
                            AuthService authService) {
        this.userMapper = userMapper;
        this.credentialMapper = credentialMapper;
        this.userAttributeValueMapper = userAttributeValueMapper;
        this.thirdPartyBindingMapper = thirdPartyBindingMapper;
        this.verificationCodeMapper = verificationCodeMapper;
        this.groupMapper = groupMapper;
        this.userGroupMapper = userGroupMapper;
        this.permissionMapper = permissionMapper;
        this.groupPermissionMapper = groupPermissionMapper;
        this.authService = authService;
    }

    /**
     * 分页查询用户列表
     */
    public Map<String, Object> listUsers(String keyword, String status, int page, int pageSize) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(User::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getEmail, keyword)
                    .or().like(User::getMobile, keyword));
        }
        wrapper.orderByAsc(User::getId);

        Page<User> userPage = userMapper.selectPage(new Page<>(page, pageSize), wrapper);

        List<Map<String, Object>> users = userPage.getRecords().stream()
                .map(this::toUserProfile)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", users);
        result.put("total", userPage.getTotal());
        result.put("page", page);
        result.put("page_size", pageSize);
        return result;
    }

    /**
     * 查询用户详情
     */
    public Map<String, Object> getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "user not found: " + userId);
        }
        return toUserProfile(user);
    }

    /**
     * 后台创建用户
     */
    @Transactional
    public Map<String, Object> createUser(String username, String email, String mobile,
                                           String password, Map<String, Object> attributes) {
        return authService.register(username, email, mobile, password, attributes);
    }

    /**
     * 修改用户信息
     */
    @Transactional
    public Map<String, Object> updateUser(Long userId, String username, String email,
                                           String mobile, Map<String, Object> attributes) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "user not found: " + userId);
        }

        if (username != null && !username.isBlank() && !username.equals(user.getUsername())) {
            Long count = credentialMapper.selectCount(
                    new LambdaQueryWrapper<Credential>()
                            .eq(Credential::getCredentialType, "USERNAME")
                            .eq(Credential::getCredentialValue, username));
            if (count > 0) throw new BusinessException(409, "username already exists");
            user.setUsername(username);
        }
        if (email != null && !email.isBlank()) {
            user.setEmail(email);
        }
        if (mobile != null && !mobile.isBlank()) {
            user.setMobile(mobile);
        }
        userMapper.updateById(user);

        if (attributes != null && !attributes.isEmpty()) {
            userAttributeValueMapper.delete(
                    new LambdaQueryWrapper<UserAttributeValue>()
                            .eq(UserAttributeValue::getUserId, userId));
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                UserAttributeValue uav = new UserAttributeValue();
                uav.setUserId(userId);
                uav.setAttrKey(entry.getKey());
                uav.setAttrValue("{\"value\":\"" + entry.getValue() + "\"}");
                userAttributeValueMapper.insert(uav);
            }
        }

        return toUserProfile(user);
    }

    /**
     * 启用用户
     */
    public void enableUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "user not found");
        user.setStatus("ENABLED");
        userMapper.updateById(user);
    }

    /**
     * 停用用户
     */
    public void disableUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "user not found");
        user.setStatus("DISABLED");
        userMapper.updateById(user);
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "user not found");
        userAttributeValueMapper.delete(
                new LambdaQueryWrapper<UserAttributeValue>().eq(UserAttributeValue::getUserId, userId));
        credentialMapper.delete(
                new LambdaQueryWrapper<Credential>().eq(Credential::getUserId, userId));
        thirdPartyBindingMapper.delete(
                new LambdaQueryWrapper<ThirdPartyBinding>().eq(ThirdPartyBinding::getUserId, userId));
        userMapper.deleteById(userId);
    }

    /**
     * 管理员重置密码
     */
    public void resetPassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "user not found");

        String effectivePassword = newPassword == null || newPassword.isBlank() ? "123456" : newPassword;
        Credential primaryCred = credentialMapper.selectOne(
                new LambdaQueryWrapper<Credential>()
                        .eq(Credential::getUserId, userId)
                        .eq(Credential::getIsPrimary, 1));
        if (primaryCred != null) {
            String newSalt = UserRealm.generateSalt();
            String newHash = UserRealm.sha256(newSalt + effectivePassword);
            primaryCred.setPasswordHash(newHash);
            primaryCred.setSalt(newSalt);
            credentialMapper.updateById(primaryCred);
        }
    }

    public Map<String, Object> listGroups(String keyword, int page, int pageSize) {
        LambdaQueryWrapper<Group> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Group::getName, keyword).or().like(Group::getDescription, keyword);
        }
        Page<Group> groupPage = groupMapper.selectPage(new Page<>(page, pageSize), wrapper.orderByAsc(Group::getId));
        return Map.of("group_list", groupPage.getRecords(), "total", groupPage.getTotal(),
                "page", page, "page_size", pageSize);
    }

    public Map<String, Object> createGroup(String name, String description) {
        if (groupMapper.selectCount(new LambdaQueryWrapper<Group>().eq(Group::getName, name)) > 0) {
            throw new BusinessException(409, "group name already exists");
        }
        Group group = new Group();
        group.setName(name);
        group.setDescription(description);
        groupMapper.insert(group);
        return Map.of("group_id", group.getId());
    }

    public Map<String, Object> updateGroup(Long groupId, String name, String description) {
        Group group = requireGroup(groupId);
        if (name != null && !name.isBlank()) group.setName(name);
        if (description != null) group.setDescription(description);
        groupMapper.updateById(group);
        return Map.of("updated", true);
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        requireGroup(groupId);
        userGroupMapper.delete(new LambdaQueryWrapper<UserGroup>().eq(UserGroup::getGroupId, groupId));
        groupPermissionMapper.delete(new LambdaQueryWrapper<GroupPermission>().eq(GroupPermission::getGroupId, groupId));
        groupMapper.deleteById(groupId);
    }

    public void addUserToGroup(Long groupId, Long userId) {
        requireGroup(groupId);
        if (userMapper.selectById(userId) == null) throw new BusinessException(404, "user not found");
        Long count = userGroupMapper.selectCount(new LambdaQueryWrapper<UserGroup>()
                .eq(UserGroup::getGroupId, groupId)
                .eq(UserGroup::getUserId, userId));
        if (count == 0) {
            UserGroup userGroup = new UserGroup();
            userGroup.setGroupId(groupId);
            userGroup.setUserId(userId);
            userGroupMapper.insert(userGroup);
        }
    }

    public void removeUserFromGroup(Long groupId, Long userId) {
        userGroupMapper.delete(new LambdaQueryWrapper<UserGroup>()
                .eq(UserGroup::getGroupId, groupId)
                .eq(UserGroup::getUserId, userId));
    }

    public Map<String, Object> groupUsers(Long groupId, int page, int pageSize) {
        requireGroup(groupId);
        List<Long> userIds = userGroupMapper.selectList(new LambdaQueryWrapper<UserGroup>()
                        .eq(UserGroup::getGroupId, groupId))
                .stream()
                .map(UserGroup::getUserId)
                .toList();
        List<Map<String, Object>> users = userIds.stream()
                .map(userMapper::selectById)
                .filter(user -> user != null)
                .map(this::toUserProfile)
                .toList();
        int from = Math.min(Math.max(0, (page - 1) * pageSize), users.size());
        int to = Math.min(from + pageSize, users.size());
        return Map.of("user_list", users.subList(from, to), "total", users.size(),
                "page", page, "page_size", pageSize);
    }

    public Map<String, Object> listPermissions() {
        List<Permission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getId));
        return Map.of("permission_list", permissions, "total", permissions.size());
    }

    public Map<String, Object> createPermission(String code, String name, String resourceType, String description) {
        if (permissionMapper.selectCount(new LambdaQueryWrapper<Permission>().eq(Permission::getCode, code)) > 0) {
            throw new BusinessException(409, "permission code already exists");
        }
        Permission permission = new Permission();
        permission.setCode(code);
        permission.setName(name);
        permission.setResourceType(resourceType);
        permission.setDescription(description);
        permissionMapper.insert(permission);
        return Map.of("permission_id", permission.getId());
    }

    public void deletePermission(Long permissionId) {
        requirePermission(permissionId);
        groupPermissionMapper.delete(new LambdaQueryWrapper<GroupPermission>()
                .eq(GroupPermission::getPermissionId, permissionId));
        permissionMapper.deleteById(permissionId);
    }

    public Map<String, Object> groupPermissions(Long groupId) {
        requireGroup(groupId);
        List<Permission> permissions = groupPermissionMapper.selectList(new LambdaQueryWrapper<GroupPermission>()
                        .eq(GroupPermission::getGroupId, groupId))
                .stream()
                .map(GroupPermission::getPermissionId)
                .map(permissionMapper::selectById)
                .filter(permission -> permission != null)
                .toList();
        return Map.of("permission_list", permissions, "total", permissions.size());
    }

    public void grantPermissions(Long groupId, List<Long> permissionIds) {
        requireGroup(groupId);
        for (Long permissionId : permissionIds) {
            requirePermission(permissionId);
            Long count = groupPermissionMapper.selectCount(new LambdaQueryWrapper<GroupPermission>()
                    .eq(GroupPermission::getGroupId, groupId)
                    .eq(GroupPermission::getPermissionId, permissionId));
            if (count == 0) {
                GroupPermission gp = new GroupPermission();
                gp.setGroupId(groupId);
                gp.setPermissionId(permissionId);
                groupPermissionMapper.insert(gp);
            }
        }
    }

    public void revokePermission(Long groupId, Long permissionId) {
        groupPermissionMapper.delete(new LambdaQueryWrapper<GroupPermission>()
                .eq(GroupPermission::getGroupId, groupId)
                .eq(GroupPermission::getPermissionId, permissionId));
    }

    /**
     * 获取调试状态
     */
    public Map<String, Object> debugState() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", userMapper.selectList(null));
        result.put("credentials", credentialMapper.selectList(null));
        result.put("verificationCodes", verificationCodeMapper.selectList(null));
        result.put("thirdPartyBindings", thirdPartyBindingMapper.selectList(null));
        return result;
    }

    /**
     * 重置调试数据
     */
    @Transactional
    public void reset() {
        userAttributeValueMapper.delete(new LambdaQueryWrapper<>());
        thirdPartyBindingMapper.delete(new LambdaQueryWrapper<>());
        credentialMapper.delete(new LambdaQueryWrapper<>());
        userGroupMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

        // 创建默认用户
        authService.register("admin", "admin@example.com", "13800000000",
                "123456", Map.of("role", "admin", "college", "software"));
        authService.register("testuser", "test@example.com", "13900000000",
                "123456", Map.of("role", "user", "studentNo", "2024090902018"));

        // admin 加入 admin 组
        User adminUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));
        if (adminUser != null) {
            Group adminGroup = groupMapper.selectOne(
                    new LambdaQueryWrapper<Group>().eq(Group::getName, "admin"));
            if (adminGroup != null) {
                UserGroup ug = new UserGroup();
                ug.setUserId(adminUser.getId());
                ug.setGroupId(adminGroup.getId());
                userGroupMapper.insert(ug);
            }
        }
    }

    private Map<String, Object> toUserProfile(User user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("mobile", user.getMobile());
        profile.put("status", user.getStatus());

        Map<String, Object> attrs = new LinkedHashMap<>();
        var attrValues = userAttributeValueMapper.selectList(
                new LambdaQueryWrapper<UserAttributeValue>()
                        .eq(UserAttributeValue::getUserId, user.getId()));
        for (UserAttributeValue uav : attrValues) {
            try {
                String json = uav.getAttrValue();
                String value = json.replace("{\"value\":\"", "").replace("\"}", "");
                attrs.put(uav.getAttrKey(), value);
            } catch (Exception ignored) {
                attrs.put(uav.getAttrKey(), uav.getAttrValue());
            }
        }
        profile.put("attributes", attrs);
        profile.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        profile.put("updatedAt", user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null);
        return profile;
    }

    private Group requireGroup(Long groupId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null) throw new BusinessException(404, "group not found: " + groupId);
        return group;
    }

    private Permission requirePermission(Long permissionId) {
        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null) throw new BusinessException(404, "permission not found: " + permissionId);
        return permission;
    }
}
