package com.onlineexam.auth.controller;

import com.onlineexam.auth.dto.ResetPasswordRequest;
import com.onlineexam.auth.dto.UserCreateRequest;
import com.onlineexam.auth.dto.UserUpdateRequest;
import com.onlineexam.auth.service.UserService;
import com.onlineexam.auth.vo.UserVO;
import com.onlineexam.common.PageResult;
import com.onlineexam.common.RequireRole;
import com.onlineexam.common.Result;
import com.onlineexam.common.Roles;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口，仅管理员可访问。
 */
@RestController
@RequestMapping("/api/users")
@RequireRole(Roles.ADMIN)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户分页查询。
     * GET /api/users?page=1&size=10&keyword=&roleCode=
     */
    @GetMapping
    public Result<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String roleCode) {
        return Result.ok(userService.page(page, size, keyword, roleCode));
    }

    /**
     * 新增用户。
     * POST /api/users
     */
    @PostMapping
    public Result<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.ok(userService.create(request));
    }

    /**
     * 修改用户（姓名、角色、状态）。
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        userService.update(id, request);
        return Result.ok();
    }

    /**
     * 重置用户密码。
     * PUT /api/users/{id}/password
     */
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return Result.ok();
    }
}
