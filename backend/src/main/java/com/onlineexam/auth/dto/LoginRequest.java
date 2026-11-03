package com.onlineexam.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求体。
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 32, message = "用户名长度不能超过32位")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(max = 64, message = "密码长度不能超过64位")
        String password
) {
}
