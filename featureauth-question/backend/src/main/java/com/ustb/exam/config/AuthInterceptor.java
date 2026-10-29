package com.ustb.exam.config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.ustb.exam.common.BizException;
import com.ustb.exam.common.JwtUtil;
import com.ustb.exam.common.LoginUser;
import com.ustb.exam.common.RequireRole;
import com.ustb.exam.common.ResultCode;
import com.ustb.exam.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

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

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
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
