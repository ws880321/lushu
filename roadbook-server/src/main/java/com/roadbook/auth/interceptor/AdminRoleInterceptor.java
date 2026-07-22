package com.roadbook.auth.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roadbook.common.ApiResponse;
import com.roadbook.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminRoleInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public AdminRoleInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        Object role = request.getAttribute("role");
        if (!"admin".equals(role)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.error(ErrorCode.FORBIDDEN, "需要管理员权限")));
            return false;
        }
        return true;
    }
}
