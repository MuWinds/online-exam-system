package com.ustb.exam.auth.controller;

import com.ustb.exam.auth.dto.LoginRequest;
import com.ustb.exam.auth.dto.PasswordChangeRequest;
import com.ustb.exam.auth.service.AuthService;
import com.ustb.exam.auth.vo.LoginVO;
import com.ustb.exam.auth.vo.UserVO;
import com.ustb.exam.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录认证接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 登录并返回 Token。
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /**
     * 获取当前登录用户。
     * GET /api/auth/current-user
     */
    @GetMapping("/current-user")
    public Result<UserVO> currentUser() {
        return Result.ok(authService.currentUser());
    }

    /**
     * 退出登录。JWT 为无状态令牌，服务端不保存会话，前端清除本地 Token 即可。
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }

    /**
     * 修改本人密码。
     * PUT /api/auth/password
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(request);
        return Result.ok();
    }
}
