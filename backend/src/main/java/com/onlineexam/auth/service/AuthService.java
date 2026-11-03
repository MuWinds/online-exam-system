package com.onlineexam.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.onlineexam.auth.dto.LoginRequest;
import com.onlineexam.auth.dto.PasswordChangeRequest;
import com.onlineexam.auth.entity.SysRole;
import com.onlineexam.auth.entity.SysUser;
import com.onlineexam.auth.mapper.RoleMapper;
import com.onlineexam.auth.mapper.UserMapper;
import com.onlineexam.auth.vo.LoginVO;
import com.onlineexam.auth.vo.UserVO;
import com.onlineexam.common.BizException;
import com.onlineexam.common.JwtUtil;
import com.onlineexam.common.LoginUser;
import com.onlineexam.common.ResultCode;
import com.onlineexam.common.UserContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录认证服务：校验账号密码、签发 Token、加载当前用户、修改本人密码。
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, RoleMapper roleMapper,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginVO login(LoginRequest request) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.username().trim()));

        // 账号不存在与密码错误使用同一提示，避免被用于枚举账号
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        SysRole role = roleMapper.selectById(user.getRoleId());
        if (role == null) {
            throw new BizException(ResultCode.ERROR, "该用户未分配有效角色，请联系管理员");
        }

        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRealName(), role.getCode());
        String token = jwtUtil.generate(loginUser);
        return new LoginVO(token, "Bearer", jwtUtil.getExpireSeconds(), UserVO.from(user, role));
    }

    public UserVO currentUser() {
        LoginUser loginUser = UserContext.require();
        SysUser user = userMapper.selectById(loginUser.id());
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户不存在或已被删除");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        return UserVO.from(user, roleMapper.selectById(user.getRoleId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(PasswordChangeRequest request) {
        LoginUser loginUser = UserContext.require();
        SysUser user = userMapper.selectById(loginUser.id());
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户不存在或已被删除");
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST, "原密码不正确");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST, "新密码不能与原密码相同");
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(update);
    }
}
