package com.ustb.exam.auth.vo;

/**
 * 登录成功返回体：Token + 当前用户信息。
 */
public record LoginVO(
        String token,
        String tokenType,
        long expiresIn,
        UserVO user
) {
}
