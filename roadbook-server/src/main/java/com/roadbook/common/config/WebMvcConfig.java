package com.roadbook.common.config;

import com.roadbook.auth.interceptor.AdminRoleInterceptor;
import com.roadbook.auth.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final AdminRoleInterceptor adminRoleInterceptor;

    @Value("${app.upload.dir:/opt/roadbook/uploads}")
    private String uploadDir;

    public WebMvcConfig(JwtInterceptor jwtInterceptor,
                       AdminRoleInterceptor adminRoleInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
        this.adminRoleInterceptor = adminRoleInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(false);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/v1/auth/**", "/api/v1/share/**", "/api/v1/upload");
        // Admin role check for all admin endpoints
        registry.addInterceptor(adminRoleInterceptor)
                .addPathPatterns("/api/v1/admin/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded files publicly
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
