package com.example.userservice.shiro;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.userservice.entity.*;
import com.example.userservice.mapper.*;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserRealm extends AuthorizingRealm {

    @Autowired
    private CredentialMapper credentialMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserGroupMapper userGroupMapper;

    @Autowired
    private GroupPermissionMapper groupPermissionMapper;

    @Autowired
    private UserPermissionMapper userPermissionMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof UsernamePasswordToken;
    }

    /**
     * 认证：校验用户凭证
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String credential = upToken.getUsername();
        String password = new String(upToken.getPassword());

        // 查询凭证：支持 username / email / mobile
        Credential dbCredential = findCredential(credential);
        if (dbCredential == null) {
            throw new UnknownAccountException("用户不存在: " + credential);
        }

        // 校验密码 SHA-256(salt + password)
        if (dbCredential.getPasswordHash() == null || dbCredential.getSalt() == null) {
            throw new IncorrectCredentialsException("密码未设置");
        }
        String hashedInput = sha256(dbCredential.getSalt() + password);
        if (!hashedInput.equals(dbCredential.getPasswordHash())) {
            throw new IncorrectCredentialsException("密码错误");
        }

        // 加载用户信息
        User user = userMapper.selectById(dbCredential.getUserId());
        if (user == null) {
            throw new UnknownAccountException("用户数据异常");
        }
        if ("DISABLED".equals(user.getStatus())) {
            throw new LockedAccountException("账号已被禁用");
        }

        return new SimpleAuthenticationInfo(user.getId(), password, getName());
    }

    /**
     * 授权：加载角色和权限
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        Long userId = (Long) principals.getPrimaryPrincipal();
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();

        // 查询用户所在的用户组
        List<UserGroup> userGroups = userGroupMapper.selectList(
                new LambdaQueryWrapper<UserGroup>().eq(UserGroup::getUserId, userId));
        Set<Long> groupIds = userGroups.stream().map(UserGroup::getGroupId).collect(Collectors.toSet());

        // 获取用户组名称作为角色
        if (!groupIds.isEmpty()) {
            List<Group> groups = groupMapper.selectBatchIds(groupIds);
            info.setRoles(groups.stream().map(Group::getName).collect(Collectors.toSet()));

            // 获取用户组拥有的权限
            List<GroupPermission> gpList = groupPermissionMapper.selectList(
                    new LambdaQueryWrapper<GroupPermission>().in(GroupPermission::getGroupId, groupIds));
            Set<Long> permissionIds = gpList.stream().map(GroupPermission::getPermissionId).collect(Collectors.toSet());

            if (!permissionIds.isEmpty()) {
                List<Permission> permissions = permissionMapper.selectBatchIds(permissionIds);
                info.setStringPermissions(permissions.stream().map(Permission::getCode).collect(Collectors.toSet()));
            }
        }

        // 直接授予用户的权限
        List<UserPermission> upList = userPermissionMapper.selectList(
                new LambdaQueryWrapper<UserPermission>().eq(UserPermission::getUserId, userId));
        if (!upList.isEmpty()) {
            Set<Long> directPermIds = upList.stream().map(UserPermission::getPermissionId).collect(Collectors.toSet());
            List<Permission> directPerms = permissionMapper.selectBatchIds(directPermIds);
            Set<String> existingPerms = info.getStringPermissions() != null
                    ? new HashSet<>(info.getStringPermissions()) : new HashSet<>();
            directPerms.forEach(p -> existingPerms.add(p.getCode()));
            info.setStringPermissions(existingPerms);
        }

        return info;
    }

    /**
     * 根据唯一凭证查找 Credential 记录
     */
    private Credential findCredential(String credential) {
        if (credential == null || credential.isBlank()) {
            return null;
        }
        // 按 USERNAME 查询
        Credential cred = credentialMapper.selectOne(
                new LambdaQueryWrapper<Credential>()
                        .eq(Credential::getCredentialType, "USERNAME")
                        .eq(Credential::getCredentialValue, credential));
        if (cred != null) return cred;

        // 按 EMAIL 查询
        cred = credentialMapper.selectOne(
                new LambdaQueryWrapper<Credential>()
                        .eq(Credential::getCredentialType, "EMAIL")
                        .eq(Credential::getCredentialValue, credential));
        if (cred != null) return cred;

        // 按 MOBILE 查询
        return credentialMapper.selectOne(
                new LambdaQueryWrapper<Credential>()
                        .eq(Credential::getCredentialType, "MOBILE")
                        .eq(Credential::getCredentialValue, credential));
    }

    /**
     * SHA-256 哈希
     */
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 生成随机盐值
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}