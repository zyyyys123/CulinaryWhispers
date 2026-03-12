package com.zyyyys.culinarywhispers.common.context;

import com.zyyyys.culinarywhispers.common.security.authz.Role;

import java.util.EnumSet;

/**
 * 用户上下文
 * 使用 ThreadLocal 存储当前请求的用户信息
 * @author zyyyys
 */
public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<EnumSet<Role>> ROLES = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void setUsername(String username) {
        USERNAME.set(username);
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static void setRoles(EnumSet<Role> roles) {
        ROLES.set(roles);
    }

    public static EnumSet<Role> getRoles() {
        EnumSet<Role> s = ROLES.get();
        return s == null ? EnumSet.of(Role.USER) : s;
    }

    public static boolean hasRole(Role role) {
        return getRoles().contains(role);
    }

    public static boolean isAdmin() {
        return hasRole(Role.ADMIN) || hasRole(Role.SUPERADMIN);
    }

    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
        ROLES.remove();
    }
}
