package com.onlineexam.config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.onlineexam.common.BizException;
import com.onlineexam.common.JwtUtil;
import com.onlineexam.common.LoginUser;
import com.onlineexam.common.RequireRole;
import com.onlineexam.common.ResultCode;
import com.onlineexam.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.List;

/**
 * 登录与鉴权拦截器：
 * 1. 解析 Authorization: Bearer &lt;token&gt;，校验失败返回 401；
 * 2. 校验 @RequireRole 声明的角色，不满足返回 403；
 * 3. 把当前用户写入 UserContext，供 Service 层使用。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 无需登录即可访问的接口 */
    private static final Set<String> WHITE_LIST = Set.of("/api/auth/login");
    private static final List<String> DEMO_GATEWAY_PATHS = List.of(
            "/api/exams", "/api/attempts", "/api/scores", "/api/papers",
            "/api/managed", "/api/paper-questions");

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final org.springframework.core.env.Environment environment;

    public AuthInterceptor(JwtUtil jwtUtil, org.springframework.core.env.Environment environment) {
        this.jwtUtil = jwtUtil;
        this.environment = environment;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (WHITE_LIST.contains(request.getRequestURI())) {
            return true;
        }
        if (List.of(environment.getActiveProfiles()).contains("demo")
                && DEMO_GATEWAY_PATHS.stream().anyMatch(request.getRequestURI()::startsWith)) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new BizException(ResultCode.UNAUTHORIZED, "未登录，请先登录");
        }

        LoginUser loginUser;
        try {
            loginUser = jwtUtil.parse(header.substring(BEARER_PREFIX.length()).trim());
        } catch (JWTVerificationException | IllegalArgumentException e) {
            throw new BizException(ResultCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }

        UserContext.set(loginUser);

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null && !contains(requireRole.value(), loginUser.role())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限访问该资源");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean contains(String[] roles, String role) {
        for (String item : roles) {
            if (item.equals(role)) {
                return true;
            }
        }
        return false;
    }
}
