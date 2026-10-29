package com.onlineexam.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.onlineexam.auth.entity.SysRole;
import com.onlineexam.auth.entity.SysUser;
import com.onlineexam.auth.mapper.RoleMapper;
import com.onlineexam.auth.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("docker")
public class DockerDemoAccountSeeder implements ApplicationRunner {
    private final RoleMapper roles;
    private final UserMapper users;
    private final PasswordEncoder passwords;

    @Value("${app.demo-password:123456}")
    private String demoPassword;

    public DockerDemoAccountSeeder(RoleMapper roles, UserMapper users, PasswordEncoder passwords) {
        this.roles = roles;
        this.users = users;
        this.passwords = passwords;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRole(1L, "ADMIN", "管理员");
        seedRole(2L, "TEACHER", "教师");
        seedRole(3L, "STUDENT", "学生");
        seedUser(1L, "teacher", "演示教师", 2L);
        seedUser(2L, "admin", "演示管理员", 1L);
        seedUser(100L, "student1", "演示学生一", 3L);
        seedUser(101L, "student2", "演示学生二", 3L);
    }

    private void seedRole(Long id, String code, String name) {
        SysRole existing = roles.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, code));
        if (existing != null) {
            existing.setName(name);
            existing.setDescription("仅供本地 Docker 演示");
            roles.updateById(existing);
            return;
        }
        SysRole role = new SysRole();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        role.setDescription("仅供本地 Docker 演示");
        roles.insert(role);
    }

    private void seedUser(Long id, String username, String realName, Long roleId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(passwords.encode(demoPassword));
        user.setRealName(realName);
        user.setRoleId(roleId);
        user.setStatus(1);
        user.setDeleted(0);
        if (users.selectById(id) == null) users.insert(user);
        else users.updateById(user);
    }
}
