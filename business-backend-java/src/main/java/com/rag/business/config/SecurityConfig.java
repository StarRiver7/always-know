package com.rag.business.config;

import com.rag.business.filter.JwtAuthenticationFilter;
import com.rag.business.security.RestAccessDeniedHandler;
import com.rag.business.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（前后端分离不需要）
            .csrf(AbstractHttpConfigurer::disable)
            
            // 配置 CORS（使用 WebConfig 中的配置）
            .cors(cors -> {})
            
            // 配置 Session 管理为无状态（不使用 HttpSession）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 配置请求授权规则
            .authorizeHttpRequests(auth -> auth
                // 允许登录、注册、健康检查接口匿名访问
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/health").permitAll()
                // OPTIONS 请求（CORS 预检）允许匿名访问
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            
            // 配置异常处理
            .exceptionHandling(exception -> exception
                // 未登录或 Token 无效时的处理
                .authenticationEntryPoint(authenticationEntryPoint)
                // 权限不足时的处理
                .accessDeniedHandler(accessDeniedHandler)
            )
                //  Spring Security 默认过滤链
                // 1. WebAsyncManagerIntegrationFilter  ← 异步处理集成
                // 2. SecurityContextPersistenceFilter  ← 安全上下文持久化
                // 3. HeaderWriterFilter                ← 头写入过滤器
                // 4. CorsFilter                        ← 跨域处理跨域处理跨域处理
                // 5. CsrfFilter                        ← CSRF防护CSRF防护CSRF防护
                // 6. LogoutFilter                      ← 登出处理登出处理登出处理
                // 把 JwtAuthenticationFilter            ← 插入到过滤链中，用于处理 JWT 认证
                // 7. UsernamePasswordAuthenticationFilter      ← 用户名密码认证
                // 8. DefaultLoginPageGeneratingFilter       ← 登录页面生成过滤器
                // 9. DefaultLogoutPageGeneratingFilter      ← 登出页面生成过滤器
                // 10. RequestCacheAwareFilter               ← 请求缓存意识过滤器
                // 11. SecurityContextHolderAwareRequestFilter  ← 安全上下文感知请求过滤器
                // 12. AnonymousAuthenticationFilter    ← 匿名认证过滤器
                // 13. SessionManagementFilter          ← 会话管理过滤器
                // 14. ExceptionTranslationFilter       ← 异常翻译过滤器
                // 15. AuthorizationFilter              ← 权限检查
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    /**
     * 密码加密器
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
