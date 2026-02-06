package com.zyyyys.culinarywhispers.common.context;

/**
 * 用户上下文
 * 使用 ThreadLocal 存储当前请求的用户信息
 * @author zyyyys
 */
public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
