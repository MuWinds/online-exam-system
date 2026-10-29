package com.onlineexam.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色要求，标注在 Controller 类或方法上。
 * 由 AuthInterceptor 在鉴权阶段校验，前端隐藏菜单不能替代该校验。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 允许访问的角色编码，满足其一即可。
     */
    String[] value();
}
