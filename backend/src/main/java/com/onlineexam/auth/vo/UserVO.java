package com.onlineexam.auth.vo;

import com.onlineexam.auth.entity.SysRole;
import com.onlineexam.auth.entity.SysUser;

import java.time.LocalDateTime;

/**
 * 用户信息视图对象（不含密码）。
 */
public record UserVO(
        Long id,
        String username,
        String realName,
        String roleCode,
        String roleName,
        Integer status,
        LocalDateTime createdAt
) {

    public static UserVO from(SysUser user, SysRole role) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                role == null ? null : role.getCode(),
                role == null ? null : role.getName(),
                user.getStatus(),
                user.getCreatedAt());
    }
}
