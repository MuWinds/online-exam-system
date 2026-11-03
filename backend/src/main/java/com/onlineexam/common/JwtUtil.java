package com.onlineexam.common;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

/**
 * JWT 生成与解析工具。密钥、有效期均来自配置文件，不在代码中硬编码。
 */
@Component
public class JwtUtil {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long expireSeconds;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-seconds:7200}") long expireSeconds) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalStateException("app.jwt.secret 长度不能小于 16 位，请通过 JWT_SECRET 环境变量配置");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).build();
        this.expireSeconds = expireSeconds;
    }

    public String generate(LoginUser user) {
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(String.valueOf(user.id()))
                .withClaim("username", user.username())
                .withClaim("realName", user.realName())
                .withClaim("role", user.role())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(expireSeconds)))
                .sign(algorithm);
    }

    /**
     * 校验并解析 Token，失败时抛出 JWTVerificationException。
     */
    public LoginUser parse(String token) {
        DecodedJWT jwt = verifier.verify(token);
        return new LoginUser(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaim("username").asString(),
                jwt.getClaim("realName").asString(),
                jwt.getClaim("role").asString());
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }
}
