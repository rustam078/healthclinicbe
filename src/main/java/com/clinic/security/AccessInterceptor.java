package com.clinic.security;

import com.clinic.enums.Access;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Enforces {@link ModuleAccess}: GET needs READ, every other method needs WRITE. */
@Component
@RequiredArgsConstructor
public class AccessInterceptor implements HandlerInterceptor {

    private final AccessGuard accessGuard;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        ModuleAccess rule = ruleFor(handler);
        boolean read = "GET".equalsIgnoreCase(request.getMethod());
        if (rule == null || (read && rule.readOpen())) {
            return true;
        }
        if (rule.adminOnly()) {
            accessGuard.requireAdmin();
        }
        accessGuard.require(rule.value(), read ? Access.READ : Access.WRITE);
        return true;
    }

    private ModuleAccess ruleFor(Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return null;
        }
        ModuleAccess rule = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), ModuleAccess.class);
        return rule != null ? rule : AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), ModuleAccess.class);
    }
}
