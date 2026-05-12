package com.rag.business.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.business.dto.response.Result;
import com.rag.business.dto.response.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 认证入口点 - 处理未登录或 Token 无效的情况
 * <p>
 * 当用户访问需要认证的接口但未提供有效 Token 时触发
 * </p>
 *
 * @author RAG Business Team
 * @since 2026-05-12
 */
@Slf4j
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        
        log.warn("认证失败，请求路径: {}, 原因: {}", request.getRequestURI(), authException.getMessage());

        // 设置响应状态码和内容类型
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 构建统一响应格式
        Result<Void> result = Result.error(
            ResultCode.UNAUTHORIZED,
            "未登录或 Token 无效，请先登录"
        );

        // 写入响应体
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
