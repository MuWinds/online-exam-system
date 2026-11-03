package com.onlineexam.auth.controller;

import com.onlineexam.auth.service.UserService;
import com.onlineexam.auth.vo.RoleVO;
import com.onlineexam.common.RequireRole;
import com.onlineexam.common.Result;
import com.onlineexam.common.Roles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色接口：提供固定三个角色的下拉数据。
 */
@RestController
@RequestMapping("/api/roles")
@RequireRole(Roles.ADMIN)
public class RoleController {

    private final UserService userService;

    public RoleController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 角色列表。
     * GET /api/roles
     */
    @GetMapping
    public Result<List<RoleVO>> list() {
        return Result.ok(userService.listRoles());
    }
}
