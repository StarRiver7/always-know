package com.rag.business.common;

/**
 * 角色常量类
 * <p>
 * 定义系统中的角色类型
 * </p>
 *
 * @author RAG Business Team
 * @since 2026-05-12
 */
public final class RoleConstants {

    /**
     * 私有构造函数，防止实例化
     */
    private RoleConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 管理员角色
     */
    public static final Long ADMIN = 1L;

    /**
     * 普通用户角色
     */
    public static final Long USER = 2L;
}
