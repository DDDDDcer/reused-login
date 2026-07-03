package com.example.userservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.userservice.common.BusinessException;
import com.example.userservice.entity.*;
import com.example.userservice.mapper.*;
import com.example.userservice.shiro.UserRealm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {
    private static final Set<String> SUPPORTED_THIRD_PARTY_PROVIDERS = Set.of("QQ", "WECHAT", "FEISHU");

    @Value("${third-party.oauth.qq.client-id:demo-qq-app-id}")
    private String qqClientId;

    @Value("${third-party.oauth.wechat.app-id:demo-wechat-app-id}")
    private String wechatAppId;

    @Value("${third-party.oauth.feishu.app-id:demo-feishu-app-id}")
    private String feishuAppId;

    private final UserMapper userMapper;
    private final CredentialMapper credentialMapper;
    private final VerificationCodeMapper verificationCodeMapper;
    private final ThirdPartyBindingMapper thirdPartyBindingMapper;
    private final UserGroupMapper userGroupMapper;
    private final GroupMapper groupMapper;
    private final UserAttributeValueMapper userAttributeValueMapper;

    public AuthService(UserMapper userMapper,
                       CredentialMapper credentialMapper,
                       VerificationCodeMapper verificationCodeMapper,
                       ThirdPartyBindingMapper thirdPartyBindingMapper,
                       UserGroupMapper userGroupMapper,
                       GroupMapper groupMapper,
                       UserAttributeValueMapper userAttributeValueMapper) {
        this.userMapper = userMapper;
        this.credentialMapper = credentialMapper;
        this.verificationCodeMapper = verificationCodeMapper;
        this.thirdPartyBindingMapper = thirdPartyBindingMapper;
        this.userGroupMapper = userGroupMapper;
        this.groupMapper = groupMapper;
        this.userAttributeValueMapper = userAttributeValueMapper;
    }

    /**
     * 唯一凭证密码登录 - 使用 Shiro Subject.login()
     */
    public Map<String, Object> loginByCredentialPassword(String credential, String password) {
        Credential dbCredential = findCredentialByValue(credential);
        if (dbCredential == null) {
            throw new BusinessException(404, "user not found");
        }
        if (dbCredential.getPasswordHash() == null || dbCredential.getSalt() == null) {
            throw new BusinessException(401, "password credential is not configured");
        }
        String hashedInput = UserRealm.sha256(dbCredential.getSalt() + password);
        if (!hashedInput.equals(dbCredential.getPasswordHash())) {
            throw new BusinessException(401, "invalid credential or password");
        }

        User user = userMapper.selectById(dbCredential.getUserId());
        if (user == null) {
            throw new BusinessException(404, "user not found");
        }
        if ("DISABLED".equals(user.getStatus())) {
            throw new BusinessException(403, "user is disabled");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", "mock-token-" + user.getId() + "-" + UUID.randomUUID());
        result.put("token_type", "Bearer");
        result.put("user", toUserProfile(user));
        return result;
    }

    /**
     * 邮箱验证码登录
     */
    public Map<String, Object> loginByEmailCode(String email, String code) {
        verifyCode(email, "login", code);

        Credential cred = credentialMapper.selectOne(
                new LambdaQueryWrapper<Credential>()
                        .eq(Credential::getCredentialType, "EMAIL")
                        .eq(Credential::getCredentialValue, email));
        if (cred == null) {
            throw new BusinessException(404, "邮箱未注册");
        }

        return performPasswordlessLogin(cred.getUserId());
    }

    /**
     * 手机号验证码登录
     */
    public Map<String, Object> loginByMobileCode(String mobile, String code) {
        verifyCode(mobile, "login", code);

        Credential cred = credentialMapper.selectOne(
                new LambdaQueryWrapper<Credential>()
                        .eq(Credential::getCredentialType, "MOBILE")
                        .eq(Credential::getCredentialValue, mobile));
        if (cred == null) {
            throw new BusinessException(404, "手机号未注册");
        }

        return performPasswordlessLogin(cred.getUserId());
    }

    public Map<String, Object> thirdPartyAuthorizeUrl(String provider, String redirectUri, String state) {
        String normalizedProvider = normalizeThirdPartyProvider(provider);
        String effectiveState = state == null || state.isBlank() ? UUID.randomUUID().toString() : state;
        String effectiveRedirect = redirectUri == null || redirectUri.isBlank()
                ? "http://localhost:8081/"
                : redirectUri;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", normalizedProvider);
        result.put("state", effectiveState);
        result.put("redirect_uri", effectiveRedirect);
        result.put("authorize_url", mockAuthorizeUrl(normalizedProvider, effectiveRedirect, effectiveState));
        result.put("note", "real provider authorize url; configure third-party.oauth.* app ids before production use");
        return result;
    }

    @Transactional
    public Map<String, Object> loginByThirdPartyCredential(String provider, String openId,
                                                           boolean autoRegister,
                                                           String nickname,
                                                           Map<String, Object> extraInfo) {
        String normalizedProvider = normalizeThirdPartyProvider(provider);
        if (openId == null || openId.isBlank()) {
            throw new BusinessException(400, "open_id is required");
        }

        ThirdPartyBinding binding = thirdPartyBindingMapper.selectOne(
                new LambdaQueryWrapper<ThirdPartyBinding>()
                        .eq(ThirdPartyBinding::getProvider, normalizedProvider)
                        .eq(ThirdPartyBinding::getOpenId, openId)
                        .last("LIMIT 1"));
        if (binding != null) {
            Map<String, Object> login = performPasswordlessLogin(binding.getUserId());
            login.put("provider", normalizedProvider);
            login.put("open_id", openId);
            login.put("binding_id", binding.getId());
            return login;
        }

        if (!autoRegister) {
            throw new BusinessException(404, "third party binding not found");
        }

        String username = nextThirdPartyUsername(normalizedProvider, openId);
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("thirdProvider", normalizedProvider);
        attributes.put("thirdOpenId", openId);
        if (nickname != null && !nickname.isBlank()) attributes.put("nickname", nickname);
        if (extraInfo != null) attributes.putAll(extraInfo);

        Map<String, Object> user = register(username, null, null, UUID.randomUUID().toString(), attributes);
        bindThirdParty(((Number) user.get("id")).longValue(), normalizedProvider, openId, extraInfo);

        Map<String, Object> login = performPasswordlessLogin(((Number) user.get("id")).longValue());
        login.put("provider", normalizedProvider);
        login.put("open_id", openId);
        login.put("auto_registered", true);
        return login;
    }

    /**
     * 账号密码注册
     */
    @Transactional
    public Map<String, Object> register(String username, String email, String mobile,
                                         String password, Map<String, Object> attributes) {
        ensureCredentialNotUsed(username, email, mobile, null);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setMobile(mobile);
        user.setStatus("ENABLED");
        userMapper.insert(user);

        // 创建主凭证 (USERNAME)
        String salt = UserRealm.generateSalt();
        String passwordHash = UserRealm.sha256(salt + (password != null && !password.isBlank() ? password : "123456"));

        Credential cred = new Credential();
        cred.setUserId(user.getId());
        cred.setCredentialType("USERNAME");
        cred.setCredentialValue(username);
        cred.setPasswordHash(passwordHash);
        cred.setSalt(salt);
        cred.setIsPrimary(1);
        credentialMapper.insert(cred);

        // 邮箱凭证
        if (email != null && !email.isBlank()) {
            Credential emailCred = new Credential();
            emailCred.setUserId(user.getId());
            emailCred.setCredentialType("EMAIL");
            emailCred.setCredentialValue(email);
            emailCred.setIsPrimary(0);
            credentialMapper.insert(emailCred);
        }

        // 手机号凭证
        if (mobile != null && !mobile.isBlank()) {
            Credential mobileCred = new Credential();
            mobileCred.setUserId(user.getId());
            mobileCred.setCredentialType("MOBILE");
            mobileCred.setCredentialValue(mobile);
            mobileCred.setIsPrimary(0);
            credentialMapper.insert(mobileCred);
        }

        // 默认加入 user 组
        Group defaultGroup = groupMapper.selectOne(
                new LambdaQueryWrapper<Group>().eq(Group::getName, "user"));
        if (defaultGroup != null) {
            UserGroup ug = new UserGroup();
            ug.setUserId(user.getId());
            ug.setGroupId(defaultGroup.getId());
            userGroupMapper.insert(ug);
        }

        // 保存自定义属性
        saveAttributes(user.getId(), attributes);

        return toUserProfile(user);
    }

    /**
     * 通过验证码注册
     */
    @Transactional
    public Map<String, Object> registerByCode(String username, String email, String mobile,
                                               String codeTarget, String code, String scene,
                                               Map<String, Object> attributes) {
        verifyCode(codeTarget, scene, code);
        return register(username, email, mobile, "123456", attributes);
    }

    /**
     * 注销
     */
    public void logout() {
        // Stateless mock service: clients discard the returned mock token.
    }

    /**
     * 发送验证码（模拟）
     */
    public Map<String, Object> sendCode(String target, String scene) {
        String effectiveScene = (scene == null || scene.isBlank()) ? "login" : scene;
        String code = "123456";

        VerificationCode vc = new VerificationCode();
        vc.setTarget(target);
        vc.setScene(effectiveScene);
        vc.setCode(code);
        vc.setUsed(0);
        vc.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        verificationCodeMapper.insert(vc);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("target", target);
        result.put("scene", effectiveScene);
        result.put("mock_code", code);
        result.put("note", "demo service always returns code 123456 for debugging");
        return result;
    }

    /**
     * 绑定第三方
     */
    public void bindThirdParty(Long userId, String provider, String openId) {
        bindThirdParty(userId, provider, openId, null);
    }

    public void bindThirdParty(Long userId, String provider, String openId, Map<String, Object> extraInfo) {
        userMapper.selectById(userId);
        if (provider == null || openId == null || provider.isBlank() || openId.isBlank()) {
            throw new BusinessException(400, "provider and openId are required");
        }

        ThirdPartyBinding binding = new ThirdPartyBinding();
        binding.setUserId(userId);
        binding.setProvider(normalizeThirdPartyProvider(provider));
        binding.setOpenId(openId);
        if (extraInfo != null && !extraInfo.isEmpty()) {
            binding.setExtraInfo(toSimpleJson(extraInfo));
        }
        thirdPartyBindingMapper.insert(binding);
    }

    /**
     * 解绑第三方
     */
    public void unbindThirdParty(Long userId, String provider, String openId) {
        userMapper.selectById(userId);
        thirdPartyBindingMapper.delete(
                new LambdaQueryWrapper<ThirdPartyBinding>()
                        .eq(ThirdPartyBinding::getUserId, userId)
                        .eq(ThirdPartyBinding::getProvider, normalizeThirdPartyProvider(provider))
                        .eq(ThirdPartyBinding::getOpenId, openId));
    }

    /**
     * 申请注销
     */
    public void applyCancellation(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "user not found");
        user.setStatus("PENDING_CANCEL");
        userMapper.updateById(user);
    }

    /**
     * 确认注销
     */
    public void confirmCancellation(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "user not found");
        user.setStatus("DISABLED");
        userMapper.updateById(user);
    }

    /**
     * 原密码修改密码
     */
    public void changePasswordByOldPassword(Long userId, String oldPassword, String newPassword) {
        Credential primaryCred = credentialMapper.selectOne(
                new LambdaQueryWrapper<Credential>()
                        .eq(Credential::getUserId, userId)
                        .eq(Credential::getIsPrimary, 1));
        if (primaryCred == null || primaryCred.getPasswordHash() == null) {
            throw new BusinessException(400, "用户无密码凭证");
        }

        String hashedOld = UserRealm.sha256(primaryCred.getSalt() + oldPassword);
        if (!hashedOld.equals(primaryCred.getPasswordHash())) {
            throw new BusinessException(401, "原密码错误");
        }

        String newSalt = UserRealm.generateSalt();
        String newHash = UserRealm.sha256(newSalt + newPassword);
        primaryCred.setPasswordHash(newHash);
        primaryCred.setSalt(newSalt);
        credentialMapper.updateById(primaryCred);
    }

    /**
     * 第三方验证重置密码
     */
    public void resetPasswordByThirdPartyVerify(String credential, String newPassword) {
        Credential cred = findCredentialByValue(credential);
        if (cred == null) throw new BusinessException(404, "user not found");

        String newSalt = UserRealm.generateSalt();
        String newHash = UserRealm.sha256(newSalt + newPassword);
        cred.setPasswordHash(newHash);
        cred.setSalt(newSalt);
        credentialMapper.updateById(cred);
    }

    // ========== 内部方法 ==========

    private Map<String, Object> performPasswordlessLogin(Long userId) {
        User user = userMapper.selectById(userId);
        if ("DISABLED".equals(user.getStatus())) {
            throw new BusinessException(403, "用户已被禁用");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", "mock-token-" + userId + "-" + UUID.randomUUID());
        result.put("token_type", "Bearer");
        result.put("user", toUserProfile(user));
        return result;
    }

    private void verifyCode(String target, String scene, String code) {
        VerificationCode vc = verificationCodeMapper.selectOne(
                new LambdaQueryWrapper<VerificationCode>()
                        .eq(VerificationCode::getTarget, target)
                        .eq(VerificationCode::getScene, scene != null ? scene : "login")
                        .orderByDesc(VerificationCode::getCreatedAt)
                        .last("LIMIT 1"));
        if (vc == null || vc.getUsed() == 1 || vc.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(401, "验证码无效或已过期");
        }
        if (!vc.getCode().equals(code)) {
            throw new BusinessException(401, "验证码错误; demo code is 123456 after sending");
        }
        vc.setUsed(1);
        verificationCodeMapper.updateById(vc);
    }

    private String normalizeThirdPartyProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new BusinessException(400, "provider is required");
        }
        String normalized = provider.trim().toUpperCase();
        if (!SUPPORTED_THIRD_PARTY_PROVIDERS.contains(normalized)) {
            throw new BusinessException(400, "unsupported third party provider: " + provider);
        }
        return normalized;
    }

    private String mockAuthorizeUrl(String provider, String redirectUri, String state) {
        return switch (provider) {
            case "QQ" -> "https://graph.qq.com/oauth2.0/authorize"
                    + "?response_type=code"
                    + "&client_id=" + encode(qqClientId)
                    + "&redirect_uri=" + encode(redirectUri)
                    + "&state=" + encode(state);
            case "WECHAT" -> "https://open.weixin.qq.com/connect/qrconnect"
                    + "?appid=" + encode(wechatAppId)
                    + "&redirect_uri=" + encode(redirectUri)
                    + "&response_type=code"
                    + "&scope=snsapi_login"
                    + "&state=" + encode(state)
                    + "#wechat_redirect";
            case "FEISHU" -> "https://open.feishu.cn/open-apis/authen/v1/authorize"
                    + "?app_id=" + encode(feishuAppId)
                    + "&redirect_uri=" + encode(redirectUri)
                    + "&state=" + encode(state);
            default -> throw new BusinessException(400, "unsupported third party provider: " + provider);
        };
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String nextThirdPartyUsername(String provider, String openId) {
        String base = (provider + "_" + openId).toLowerCase().replaceAll("[^a-z0-9_]", "_");
        if (base.length() > 48) base = base.substring(0, 48);
        String candidate = base;
        int index = 1;
        while (credentialMapper.selectCount(new LambdaQueryWrapper<Credential>()
                .eq(Credential::getCredentialType, "USERNAME")
                .eq(Credential::getCredentialValue, candidate)) > 0) {
            candidate = base + "_" + index++;
            if (candidate.length() > 64) {
                candidate = candidate.substring(0, 58) + "_" + index;
            }
        }
        return candidate;
    }

    private String toSimpleJson(Map<String, Object> value) {
        StringBuilder builder = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            if (index++ > 0) builder.append(",");
            builder.append("\"").append(entry.getKey()).append("\":\"")
                    .append(String.valueOf(entry.getValue()).replace("\"", "\\\""))
                    .append("\"");
        }
        builder.append("}");
        return builder.toString();
    }

    private void ensureCredentialNotUsed(String username, String email, String mobile, Long excludeUserId) {
        if (username != null && !username.isBlank()) {
            Long count = credentialMapper.selectCount(
                    new LambdaQueryWrapper<Credential>()
                            .eq(Credential::getCredentialType, "USERNAME")
                            .eq(Credential::getCredentialValue, username));
            if (count > 0) throw new BusinessException(409, "username already exists");
        }
        if (email != null && !email.isBlank()) {
            Long count = credentialMapper.selectCount(
                    new LambdaQueryWrapper<Credential>()
                            .eq(Credential::getCredentialType, "EMAIL")
                            .eq(Credential::getCredentialValue, email));
            if (count > 0) throw new BusinessException(409, "email already exists");
        }
        if (mobile != null && !mobile.isBlank()) {
            Long count = credentialMapper.selectCount(
                    new LambdaQueryWrapper<Credential>()
                            .eq(Credential::getCredentialType, "MOBILE")
                            .eq(Credential::getCredentialValue, mobile));
            if (count > 0) throw new BusinessException(409, "mobile already exists");
        }
    }

    private Credential findCredentialByValue(String credential) {
        Credential cred = credentialMapper.selectOne(
                new LambdaQueryWrapper<Credential>()
                        .eq(Credential::getCredentialType, "USERNAME")
                        .eq(Credential::getCredentialValue, credential));
        if (cred != null) return cred;
        cred = credentialMapper.selectOne(
                new LambdaQueryWrapper<Credential>()
                        .eq(Credential::getCredentialType, "EMAIL")
                        .eq(Credential::getCredentialValue, credential));
        if (cred != null) return cred;
        return credentialMapper.selectOne(
                new LambdaQueryWrapper<Credential>()
                        .eq(Credential::getCredentialType, "MOBILE")
                        .eq(Credential::getCredentialValue, credential));
    }

    private void saveAttributes(Long userId, Map<String, Object> attributes) {
        if (attributes == null) return;
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            UserAttributeValue uav = new UserAttributeValue();
            uav.setUserId(userId);
            uav.setAttrKey(entry.getKey());
            uav.setAttrValue("{\"value\":\"" + entry.getValue() + "\"}");
            userAttributeValueMapper.insert(uav);
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
}
