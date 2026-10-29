package com.onlineexam.auth.vo;

import com.onlineexam.auth.entity.SysRole;

/**
 * 角色下拉项。
 */
public record RoleVO(Long id, String code, String name) {

    public static RoleVO from(SysRole role) {
        return new RoleVO(role.getId(), role.getCode(), role.getName());
    }
}
