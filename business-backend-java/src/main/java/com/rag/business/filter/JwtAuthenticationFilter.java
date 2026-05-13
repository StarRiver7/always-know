package com.rag.business.filter;

import com.rag.business.service.TokenService;
import com.rag.business.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 认证过滤器
 * <ul>
 * 集成到 Spring Security 过滤链中，负责：
 * <li>1. 从请求头提取 Token</li>
 * <li>2. 验证 Token 有效性</li>
 * <li>3. 从 Redis 校验 Token</li>
 * <li>4. 设置 Spring Security 上下文</li>
 * </ul>
 *
 * @author RAG Business Team
 * @since 2026-05-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 从请求头获取 Token
            String token = getTokenFromRequest(request);

            // 2. 如果 Token 存在且有效
            if (StringUtils.hasText(token)) {
                // 3. 检查 Token 是否过期
                if (!jwtUtil.isTokenExpired(token)) {
                    // 4. 从 Token 中解析用户信息
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    String username = jwtUtil.getUsernameFromToken(token);

                    // 5. 从 Redis 验证 Token 是否有效（防止多地登录、踢人下线等场景）
                    if (tokenService.validateToken(userId, token)) {
                        // 6. 创建认证对象
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userId,  // principal（主体）存储 userId
                                        null,    // credentials（凭证）不需要
                                        new ArrayList<>()  // authorities（权限列表）
                                );

                        // 7. 设置请求详情
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // 8. 将用户信息存入 request attribute（供 @CurrentUserId 使用）
                        request.setAttribute("userId", userId);
                        request.setAttribute("username", username);

                        // 9. 设置到 Spring Security 上下文
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("用户认证成功: userId={}, username={}", userId, username);
                    } else {
                        log.warn("Token 验证失败（Redis中不存在或不匹配）: userId={}", userId);
                        // Token 无效，清除认证信息
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    log.warn("Token 已过期");
                    // Token 过期，清除认证信息
                    SecurityContextHolder.clearContext();
                }
            } else {
                log.debug("请求未携带 Token，路径: {}", request.getRequestURI());
            }
        } catch (Exception e) {
            log.error("JWT 认证失败: {}", e.getMessage());
            // 认证失败，清除上下文
            SecurityContextHolder.clearContext();
        }

        // 继续过滤链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
