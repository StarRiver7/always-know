package com.rag.business.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 当前登录用户ID注解
 * <p>
 * 用于在 Controller 方法参数中自动注入当前登录用户的ID
 * 替代手动从 HttpServletRequest 中获取 userId
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * @GetMapping("/documents")
 * public Result<List<Document>> list(@CurrentUserId Long userId) {
 *     // 直接使用 userId，无需手动从 request 获取
 *     return documentService.listByUserId(userId);
 * }
 * }
 * </pre>
 *
 * @author RAG Business Team
 * @since 2026-05-12
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
