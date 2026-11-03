package com.onlineexam.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 管理员修改用户请求体（账号不可修改）。
 */
public record UserUpdateRequest(

        @NotBlank(message = "姓名不能为空")
        @Size(max = 32, message = "姓名长度不能超过32位")
        String realName,

        @NotBlank(message = "角色不能为空")
        String roleCode,

        @NotNull(message = "状态不能为空")
        Integer status
) {
}
