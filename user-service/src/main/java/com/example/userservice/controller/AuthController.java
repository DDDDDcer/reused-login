package com.example.userservice.controller;

import com.example.userservice.common.ApiResponse;
import com.example.userservice.common.BusinessException;
import com.example.userservice.model.UserProfile;
import com.example.userservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth - 认证", description = "用户登录、注册、第三方绑定等认证接口")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login/credential-password")
    @Operation(summary = "唯一凭证密码登录", description = "通过唯一凭证（用户名/邮箱/手机号）和密码进行登录")
    public ApiResponse<Map<String, Object>> loginByCredentialPassword(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(authService.loginByCredentialPassword(
                required(body, "credential", "account", "username"),
                required(body, "password")
        ));
    }

    @PostMapping("/login/email-code")
    @Operation(summary = "邮箱验证码登录", description = "通过邮箱和验证码进行登录")
    public ApiResponse<Map<String, Object>> loginByEmailCode(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(authService.loginByEmailCode(
                required(body, "email"),
                required(body, "code")
        ));
    }

    @PostMapping("/login/mobile-code")
    @Operation(summary = "手机号验证码登录", description = "通过手机号和验证码进行登录")
    public ApiResponse<Map<String, Object>> loginByMobileCode(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(authService.loginByMobileCode(
                required(body, "mobile"),
                required(body, "code")
        ));
    }

    @PostMapping("/third-party/authorize-url")
    @Operation(summary = "拉起第三方登录", description = "生成 QQ / WECHAT / FEISHU 第三方授权地址和 state")
    public ApiResponse<Map<String, Object>> thirdPartyAuthorizeUrl(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(authService.thirdPartyAuthorizeUrl(
                required(body, "provider"),
                RequestMaps.stringValue(body, "redirect_uri", "redirectUri"),
                RequestMaps.stringValue(body, "state")
        ));
    }

    @PostMapping("/login/third-party-credential")
    @Operation(summary = "第三方唯一凭证登录", description = "通过 provider + open_id 登录，支持 QQ / WECHAT / FEISHU")
    public ApiResponse<Map<String, Object>> loginByThirdPartyCredential(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(authService.loginByThirdPartyCredential(
                required(body, "provider"),
                required(body, "open_id", "openId"),
                Boolean.parseBoolean(String.valueOf(body.getOrDefault("auto_register", body.getOrDefault("autoRegister", false)))),
                RequestMaps.stringValue(body, "nickname"),
                RequestMaps.mapValue(body, "extra_info")
        ));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出", description = "当前登录用户退出登录")
    public ApiResponse<Map<String, Object>> logout() {
        authService.logout();
        return ApiResponse.ok(Map.of("logout", true));
    }

    @PostMapping("/register/account-password")
    @Operation(summary = "账号密码注册", description = "通过账号和密码完成注册")
    public ApiResponse<Map<String, Object>> registerByAccountPassword(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(authService.register(
                required(body, "username", "account"),
                RequestMaps.stringValue(body, "email"),
                RequestMaps.stringValue(body, "mobile"),
                required(body, "password"),
                RequestMaps.mapValue(body, "attributes")
        ));
    }

    @PostMapping("/register/email-code")
    @Operation(summary = "邮箱验证码注册", description = "通过邮箱验证码完成注册")
    public ApiResponse<Map<String, Object>> registerByEmailCode(@RequestBody Map<String, Object> body) {
        String email = required(body, "email");
        return ApiResponse.ok(authService.registerByCode(
                RequestMaps.stringValue(body, "username", "account"),
                email,
                RequestMaps.stringValue(body, "mobile"),
                email,
                required(body, "code"),
                "register",
                RequestMaps.mapValue(body, "attributes")
        ));
    }

    @PostMapping("/register/mobile-code")
    @Operation(summary = "手机号验证码注册", description = "通过手机号验证码完成注册")
    public ApiResponse<Map<String, Object>> registerByMobileCode(@RequestBody Map<String, Object> body) {
        String mobile = required(body, "mobile");
        return ApiResponse.ok(authService.registerByCode(
                RequestMaps.stringValue(body, "username", "account"),
                RequestMaps.stringValue(body, "email"),
                mobile,
                mobile,
                required(body, "code"),
                "register",
                RequestMaps.mapValue(body, "attributes")
        ));
    }

    @PostMapping("/register/bind-third-party")
    @Operation(summary = "注册并绑定第三方应用", description = "用户注册时同时绑定第三方应用账号")
    public ApiResponse<Map<String, Object>> registerAndBindThirdParty(@RequestBody Map<String, Object> body) {
        Map<String, Object> user = authService.register(
                required(body, "username", "account"),
                RequestMaps.stringValue(body, "email"),
                RequestMaps.stringValue(body, "mobile"),
                RequestMaps.stringValue(body, "password"),
                RequestMaps.mapValue(body, "attributes")
        );
        authService.bindThirdParty(
                ((Number) user.get("id")).longValue(),
                required(body, "provider"),
                required(body, "open_id", "openId"));
        return ApiResponse.ok(user);
    }

    @PostMapping("/third-party/bind")
    @Operation(summary = "绑定第三方应用", description = "已登录用户绑定第三方应用账号")
    public ApiResponse<Map<String, Object>> bindThirdParty(@RequestBody Map<String, Object> body) {
        authService.bindThirdParty(
                requiredLong(body, "user_id", "userId"),
                required(body, "provider"),
                required(body, "open_id", "openId")
        );
        return ApiResponse.ok(Map.of("bind", true));
    }

    @PostMapping("/third-party/unbind")
    @Operation(summary = "解绑第三方应用", description = "已登录用户解绑第三方应用账号")
    public ApiResponse<Map<String, Object>> unbindThirdParty(@RequestBody Map<String, Object> body) {
        authService.unbindThirdParty(
                requiredLong(body, "user_id", "userId"),
                required(body, "provider"),
                required(body, "open_id", "openId")
        );
        return ApiResponse.ok(Map.of("unbind", true));
    }

    @PostMapping("/code/email/send")
    @Operation(summary = "发送邮箱验证码", description = "发送邮箱验证码，调试服务固定返回 123456")
    public ApiResponse<Map<String, Object>> sendEmailCode(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(authService.sendCode(required(body, "email"), RequestMaps.stringValue(body, "scene")));
    }

    @PostMapping("/code/mobile/send")
    @Operation(summary = "发送手机验证码", description = "发送手机验证码，调试服务固定返回 123456")
    public ApiResponse<Map<String, Object>> sendMobileCode(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(authService.sendCode(required(body, "mobile"), RequestMaps.stringValue(body, "scene")));
    }

    @PostMapping("/cancellation/apply")
    @Operation(summary = "发起账号注销", description = "用户提交注销申请，进入冷静期")
    public ApiResponse<Map<String, Object>> applyCancellation(@RequestBody Map<String, Object> body) {
        authService.applyCancellation(requiredLong(body, "user_id", "userId"));
        return ApiResponse.ok(Map.of("status", "PENDING_CANCEL"));
    }

    @PostMapping("/cancellation/confirm")
    @Operation(summary = "确认账号注销", description = "冷静期结束后确认注销账号")
    public ApiResponse<Map<String, Object>> confirmCancellation(@RequestBody Map<String, Object> body) {
        authService.confirmCancellation(requiredLong(body, "user_id", "userId"));
        return ApiResponse.ok(Map.of("status", "DISABLED"));
    }

    @PostMapping("/password/reset/third-party-verify")
    @Operation(summary = "第三方验证重置密码", description = "用户通过第三方身份验证后重置密码")
    public ApiResponse<Map<String, Object>> resetPasswordByThirdPartyVerify(@RequestBody Map<String, Object> body) {
        authService.resetPasswordByThirdPartyVerify(
                required(body, "credential", "email", "mobile", "username"),
                required(body, "new_password", "newPassword"));
        return ApiResponse.ok(Map.of("reset", true));
    }

    @PostMapping("/password/change/old-password")
    @Operation(summary = "原密码修改密码", description = "用户输入原密码后修改新密码")
    public ApiResponse<Map<String, Object>> changePasswordByOldPassword(@RequestBody Map<String, Object> body) {
        authService.changePasswordByOldPassword(
                requiredLong(body, "user_id", "userId"),
                required(body, "old_password", "oldPassword"),
                required(body, "new_password", "newPassword")
        );
        return ApiResponse.ok(Map.of("changed", true));
    }

    @PostMapping("/password/change/third-party-verify")
    @Operation(summary = "第三方验证修改密码", description = "用户通过第三方验证后修改密码")
    public ApiResponse<Map<String, Object>> changePasswordByThirdPartyVerify(@RequestBody Map<String, Object> body) {
        authService.resetPasswordByThirdPartyVerify(
                required(body, "credential", "email", "mobile", "username"),
                required(body, "new_password", "newPassword"));
        return ApiResponse.ok(Map.of("changed", true));
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
