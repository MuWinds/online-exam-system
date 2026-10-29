package com.ustb.exam.common;

/**
 * 当前登录用户（从 Token 解析得到），在一次请求内通过 {@link UserContext} 传递。
 *
 * @param id       用户ID
 * @param username 账号
 * @param realName 姓名
 * @param role     角色编码：ADMIN / TEACHER / STUDENT
 */
public record LoginUser(Long id, String username, String realName, String role) {
}
