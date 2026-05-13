package com.rag.business.controller;

import com.rag.business.annotation.CurrentUserId;
import com.rag.business.dto.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试接口 - 用于验证认证机制
 *
 * @author RAG Business Team
 * @since 2026-05-12
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * 测试认证 - 需要登录才能访问
     */
    @GetMapping("/auth-required")
    public Result<Map<String, Object>> testAuthRequired(@CurrentUserId Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "认证成功！");
        result.put("userId", userId);
        result.put("timestamp", System.currentTimeMillis());
        
        log.info("测试认证接口被调用，用户ID: {}", userId);
        return Result.success(result);
    }

    /**
     * 测试 SecurityContext
     */
    @GetMapping("/security-context")
    public Result<Map<String, Object>> testSecurityContext() {
        Object principal = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "SecurityContext 测试成功！");
        result.put("principal", principal);
        result.put("timestamp", System.currentTimeMillis());
        
        log.info("测试 SecurityContext 接口被调用，principal: {}", principal);
        return Result.success(result);
    }
}
