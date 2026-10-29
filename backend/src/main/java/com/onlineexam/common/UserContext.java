package com.onlineexam.common;

/**
 * 基于 ThreadLocal 的登录用户上下文。
 * 由 AuthInterceptor 在请求进入时写入、请求结束时清理。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 获取当前用户，未登录时抛出 401。
     */
    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    public static boolean hasRole(String role) {
        LoginUser user = HOLDER.get();
        return user != null && role.equals(user.role());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
