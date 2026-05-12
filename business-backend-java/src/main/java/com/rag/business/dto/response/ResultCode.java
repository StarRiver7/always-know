package com.rag.business.dto.response;

/**
 * 响应状态码常量
 * <p>
 * 定义系统统一的响应状态码，确保前后端交互的一致性
 * </p>
 *
 * <h3>状态码规范：</h3>
 * <ul>
 *   <li>2xx - 成功</li>
 *   <li>4xx - 客户端错误</li>
 *   <li>5xx - 服务器错误</li>
 *   <li>1xxx - 业务错误</li>
 * </ul>
 *
 * @author RAG Business Team
 * @since 2026-05-12
 */
public final class ResultCode {

    /**
     * 私有构造函数，防止实例化
     */
    private ResultCode() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 成功状态码 ====================

    /**
     * 请求成功
     */
    public static final Integer SUCCESS = 200;

    /**
     * 创建成功
     */
    public static final Integer CREATED = 201;

    /**
     * 请求已接受，但处理尚未完成
     */
    public static final Integer ACCEPTED = 202;


    // ==================== 客户端错误（4xx）====================

    /**
     * 请求参数错误
     */
    public static final Integer BAD_REQUEST = 400;

    /**
     * 未授权（需要登录）
     */
    public static final Integer UNAUTHORIZED = 401;

    /**
     * 禁止访问（权限不足）
     */
    public static final Integer FORBIDDEN = 403;

    /**
     * 资源不存在
     */
    public static final Integer NOT_FOUND = 404;

    /**
     * 请求方法不支持
     */
    public static final Integer METHOD_NOT_ALLOWED = 405;

    /**
     * 请求冲突（如重复提交）
     */
    public static final Integer CONFLICT = 409;

    /**
     * 请求实体过大
     */
    public static final Integer PAYLOAD_TOO_LARGE = 413;

    /**
     * 请求过于频繁（限流）
     */
    public static final Integer TOO_MANY_REQUESTS = 429;


    // ==================== 服务器错误（5xx）====================

    /**
     * 服务器内部错误
     */
    public static final Integer INTERNAL_SERVER_ERROR = 500;

    /**
     * 服务不可用
     */
    public static final Integer SERVICE_UNAVAILABLE = 503;


    // ==================== 业务错误（1xxx）====================

    /**
     * 业务逻辑错误（通用）
     */
    public static final Integer BUSINESS_ERROR = 1000;

    /**
     * 用户名或密码错误
     */
    public static final Integer LOGIN_FAILED = 1001;

    /**
     * 用户已存在
     */
    public static final Integer USER_ALREADY_EXISTS = 1002;

    /**
     * 用户不存在
     */
    public static final Integer USER_NOT_FOUND = 1003;

    /**
     * 账号已被禁用
     */
    public static final Integer USER_DISABLED = 1004;

    /**
     * Token 无效或已过期
     */
    public static final Integer TOKEN_INVALID = 1005;

    /**
     * 文件上传失败
     */
    public static final Integer UPLOAD_FAILED = 1006;

    /**
     * 文件不存在
     */
    public static final Integer FILE_NOT_FOUND = 1007;

    /**
     * 权限不足
     */
    public static final Integer PERMISSION_DENIED = 1008;

    /**
     * 数据已被删除
     */
    public static final Integer DATA_DELETED = 1009;

    /**
     * 操作过于频繁
     */
    public static final Integer OPERATION_TOO_FREQUENT = 1010;
}
