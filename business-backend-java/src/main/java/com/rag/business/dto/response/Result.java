package com.rag.business.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装
 * <p>
 * 用于包装所有 API 接口的返回结果，确保响应格式统一
 * </p>
 *
 * @param <T> 响应数据类型
 * @author RAG Business Team
 * @since 2026-05-12
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     * <ul>
     *   <li>200 - 成功</li>
     *   <li>400 - 请求参数错误</li>
     *   <li>401 - 未授权</li>
     *   <li>403 - 禁止访问</li>
     *   <li>404 - 资源不存在</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳（可选，用于前端缓存控制）
     */
    private Long timestamp;

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     */
    public static <T> Result<T> success(T data) {
        return result(200, "success", data);
    }

    /**
     * 成功响应（带消息和数据）
     *
     * @param message 响应消息
     * @param data    响应数据
     */
    public static <T> Result<T> success(String message, T data) {
        return result(200, message, data);
    }

    /**
     * 失败响应（默认 500）
     *
     * @param message 错误消息
     */
    public static <T> Result<T> error(String message) {
        return result(500, message, null);
    }

    /**
     * 失败响应（指定错误码）
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public static <T> Result<T> error(Integer code, String message) {
        return result(code, message, null);
    }

    /**
     * 失败响应（指定错误码、消息和数据）
     *
     * @param code    错误码
     * @param message 错误消息
     * @param data    响应数据
     */
    public static <T> Result<T> error(Integer code, String message, T data) {
        return result(code, message, data);
    }

    /**
     * 构建响应结果
     *
     * @param code    状态码
     * @param message 响应消息
     * @param data    响应数据
     */
    private static <T> Result<T> result(Integer code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code != null && this.code == 200;
    }
}
