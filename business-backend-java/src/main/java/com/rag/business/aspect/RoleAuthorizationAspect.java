package com.rag.business.aspect;

import com.rag.business.annotation.RequireRole;
import com.rag.business.dto.response.Result;
import com.rag.business.dto.response.ResultCode;
import com.rag.business.entity.UserRole;
import com.rag.business.mapper.UserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * 角色权限校验切面
 * <p>
 * 拦截带有 @RequireRole 注解的方法，校验用户角色权限
 * </p>
 *
 * @author RAG Business Team
 * @since 2026-05-12
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RoleAuthorizationAspect {

    private final UserRoleMapper userRoleMapper;

    /**
     * 环绕通知：拦截带有 @RequireRole 注解的方法
     */
    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        // 1. 获取当前请求
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.error("无法获取 HTTP 请求上下文");
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR, "系统错误");
        }

        HttpServletRequest request = attributes.getRequest();

        // 2. 从 request 中获取 userId（由 JwtAuthenticationFilter 设置）
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            log.warn("权限校验失败：未找到用户ID");
            return Result.error(ResultCode.UNAUTHORIZED, "请先登录");
        }

        // 3. 获取注解中要求的角色
        long[] requiredRoles = requireRole.value();
        if (requiredRoles.length == 0) {
            log.warn("权限校验失败：@RequireRole 未指定角色");
            return Result.error(ResultCode.FORBIDDEN, "权限配置错误");
        }

        // 4. 查询用户的角色
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)
                        .eq(UserRole::getDeleted, 0)
        );

        if (userRoles.isEmpty()) {
            log.warn("用户 {} 没有任何角色", userId);
            return Result.error(ResultCode.FORBIDDEN, "权限不足，请联系管理员");
        }

        // 5. 检查用户是否拥有要求的角色
        List<Long> userRoleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .toList();

        boolean hasPermission = Arrays.stream(requiredRoles)
                .anyMatch(userRoleIds::contains);

        if (!hasPermission) {
            log.warn("用户 {} 权限不足，需要角色: {}，实际角色: {}", 
                    userId, 
                    Arrays.toString(requiredRoles), 
                    userRoleIds);
            return Result.error(ResultCode.FORBIDDEN, "权限不足，无法访问该资源");
        }

        log.debug("用户 {} 角色校验通过，拥有角色: {}", userId, userRoleIds);

        // 6. 权限校验通过，继续执行方法
        return joinPoint.proceed();
    }
}
