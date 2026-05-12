package com.rag.business.resolver;

import com.rag.business.annotation.CurrentUserId;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 当前用户ID参数解析器
 * <p>
 * 解析 {@link CurrentUserId} 注解，自动从 request 中获取 userId 并注入到方法参数
 * </p>
 *
 * @author RAG Business Team
 * @since 2026-05-12
 */
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * 判断是否支持该参数类型
     *
     * @param parameter 方法参数
     * @return 如果参数有 @CurrentUserId 注解则支持
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && parameter.getParameterType().equals(Long.class);
    }

    /**
     * 解析参数值
     *
     * @param parameter     方法参数
     * @param mavContainer  模型视图容器
     * @param webRequest    Web 请求
     * @param binderFactory 数据绑定工厂
     * @return 当前登录用户的 ID
     */
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException("无法获取 HttpServletRequest");
        }

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new IllegalStateException("未找到用户ID，请先登录");
        }

        return userId;
    }
}
