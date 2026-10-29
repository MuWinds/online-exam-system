package com.onlineexam.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员重置用户密码请求体。
 */
public record ResetPasswordRequest(

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 20, message = "新密码长度必须为6~20位")
        String newPassword
) {
}
