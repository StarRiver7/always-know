package com.rag.business.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限校验注解
 * <p>
 * 用于标记需要特定角色才能访问的接口
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * // 只有管理员能访问
 * @RequireRole(1)
 * @PostMapping("/upload")
 * public Result upload() { ... }
 * 
 * // 管理员或普通用户都能访问
 * @RequireRole({1, 2})
 * @GetMapping("/documents")
 * public Result list() { ... }
 * }
 * </pre>
 *
 * <h3>角色说明：</h3>
 * <ul>
 *   <li>1 - 管理员（ADMIN）</li>
 *   <li>2 - 普通用户（USER）</li>
 * </ul>
 *
 * @author RAG Business Team
 * @since 2026-05-12
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    
    /**
     * 允许访问的角色ID数组
     * <p>
     * 只要用户拥有其中一个角色即可访问
     * </p>
     *
     * @return 角色ID数组
     */
    long[] value();
}
