package com.roadbook.auth.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roadbook.auth.service.WechatAuthService;
import com.roadbook.common.ApiResponse;
import com.roadbook.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final WechatAuthService wechatAuthService;
    private final ObjectMapper objectMapper;

    public JwtInterceptor(WechatAuthService wechatAuthService, ObjectMapper objectMapper) {
        this.wechatAuthService = wechatAuthService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI();

        // Public share API — no auth required
        if (path.startsWith("/api/v1/share/")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "Missing or invalid Authorization header");
            return false;
        }

        String token = authHeader.substring(7);

        try {
            Long userId = wechatAuthService.parseUserId(token);
            String role = wechatAuthService.parseRole(token);
            request.setAttribute("userId", userId);
            request.setAttribute("role", role);
            return true;
        } catch (Exception e) {
            writeUnauthorized(response, "Invalid or expired token");
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiResponse<Void> body = ApiResponse.error(ErrorCode.UNAUTHORIZED, message);
        response.getWriter().write(
                objectMapper.writeValueAsString(body)
        );
    }
}
