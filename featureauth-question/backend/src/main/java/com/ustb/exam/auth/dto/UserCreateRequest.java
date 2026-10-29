package com.ustb.exam.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理员新增用户请求体。
 */
public record UserCreateRequest(

        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "用户名必须为4~20位字母、数字或下划线")
        String username,

        @Size(min = 6, max = 20, message = "密码长度必须为6~20位")
        String password,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 32, message = "姓名长度不能超过32位")
        String realName,

        @NotBlank(message = "角色不能为空")
        String roleCode,

        Integer status
) {
}
