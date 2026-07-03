package com.example.userservice.shiro;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

/**
 * Shiro 工具类 - 获取当前登录用户信息
 */
public class ShiroUtils {

    /**
     * 获取当前登录用户的 Subject
     */
    public static Subject getSubject() {
        return SecurityUtils.getSubject();
    }

    /**
     * 获取当前登录用户的 userId
     */
    public static Long getCurrentUserId() {
        Subject subject = getSubject();
        Object principal = subject.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }

    /**
     * 判断当前用户是否已认证
     */
    public static boolean isAuthenticated() {
        return getSubject() != null && getSubject().isAuthenticated();
    }

    /**
     * 判断当前用户是否拥有指定角色
     */
    public static boolean hasRole(String role) {
        return getSubject() != null && getSubject().hasRole(role);
    }

    /**
     * 登出
     */
    public static void logout() {
        Subject subject = getSubject();
        if (subject != null) {
            subject.logout();
        }
    }
}