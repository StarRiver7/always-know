package com.rag.business.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.business.dto.response.Result;
import com.rag.business.dto.response.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 授权异常处理 - 处理权限不足的情况
 * <p>
 * 当用户已登录但访问没有权限的接口时触发
 * </p>
 *
 * @author RAG Business Team
 * @since 2026-05-12
 */
@Slf4j
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                      HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException {
        
        log.warn("权限不足，请求路径: {}, 用户ID: {}, 原因: {}", 
                request.getRequestURI(),
                request.getAttribute("userId"),
                accessDeniedException.getMessage());

        // 设置响应状态码和内容类型
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 构建统一响应格式
        Result<Void> result = Result.error(
            ResultCode.FORBIDDEN,
            "权限不足，无法访问该资源"
        );

        // 写入响应体
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
