package com.onlineexam.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineexam.auth.dto.ResetPasswordRequest;
import com.onlineexam.auth.dto.UserCreateRequest;
import com.onlineexam.auth.dto.UserUpdateRequest;
import com.onlineexam.auth.entity.SysRole;
import com.onlineexam.auth.entity.SysUser;
import com.onlineexam.auth.mapper.RoleMapper;
import com.onlineexam.auth.mapper.UserMapper;
import com.onlineexam.auth.vo.RoleVO;
import com.onlineexam.auth.vo.UserVO;
import com.onlineexam.common.BizException;
import com.onlineexam.common.PageResult;
import com.onlineexam.common.ResultCode;
import com.onlineexam.common.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理服务（仅管理员使用）。
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    /** 管理员新增用户时未填写密码则使用的初始密码，可在配置文件中修改 */
    @Value("${app.default-password:123456}")
    private String defaultPassword;

    public UserService(UserMapper userMapper, RoleMapper roleMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<UserVO> page(long page, long size, String keyword, String roleCode) {
        long current = page < 1 ? 1 : page;
        long pageSize = size < 1 ? 10 : Math.min(size, 100);

        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(SysUser::getUsername, kw).or().like(SysUser::getRealName, kw));
        }
        if (StringUtils.hasText(roleCode)) {
            SysRole role = findRoleByCode(roleCode);
            if (role == null) {
                return PageResult.of(List.of(), 0, current, pageSize);
            }
            wrapper.eq(SysUser::getRoleId, role.getId());
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);

        Page<SysUser> result = userMapper.selectPage(new Page<>(current, pageSize), wrapper);
        Map<Long, SysRole> roleMap = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, role -> role));
        List<UserVO> records = result.getRecords().stream()
                .map(user -> UserVO.from(user, roleMap.get(user.getRoleId())))
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public UserVO create(UserCreateRequest request) {
        String username = request.username().trim();
        Long exists = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (exists != null && exists > 0) {
            throw new BizException(ResultCode.CONFLICT, "用户名已存在");
        }
        SysRole role = requireRole(request.roleCode());

        SysUser user = new SysUser();
        user.setUsername(username);
        String rawPassword = StringUtils.hasText(request.password()) ? request.password() : defaultPassword;
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(request.realName().trim());
        user.setRoleId(role.getId());
        user.setStatus(request.status() == null ? 1 : request.status());
        user.setDeleted(0);
        userMapper.insert(user);
        return UserVO.from(user, role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserUpdateRequest request) {
        SysUser exists = userMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (UserContext.require().id().equals(id) && !Integer.valueOf(1).equals(request.status())) {
            throw new BizException(ResultCode.BAD_REQUEST, "不能禁用当前登录账号");
        }
        SysRole role = requireRole(request.roleCode());

        SysUser update = new SysUser();
        update.setId(id);
        update.setRealName(request.realName().trim());
        update.setRoleId(role.getId());
        update.setStatus(request.status());
        userMapper.updateById(update);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, ResetPasswordRequest request) {
        SysUser exists = userMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(update);
    }

    public List<RoleVO> listRoles() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getId))
                .stream()
                .map(RoleVO::from)
                .toList();
    }

    private SysRole findRoleByCode(String roleCode) {
        return roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, roleCode));
    }

    private SysRole requireRole(String roleCode) {
        SysRole role = findRoleByCode(roleCode);
        if (role == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "角色不存在：" + roleCode);
        }
        return role;
    }
}
