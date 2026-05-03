package cn.org.openygt.config;

import cn.org.openygt.common.interceptor.AuthInterceptor;
import cn.org.openygt.rbac.interceptor.PermissionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 *
 * <p>注册 JWT 认证拦截器，并配置白名单路径。</p>
 * <p>拦截范围覆盖全部 /api/**，确保无接口绕过鉴权。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",
                        "/api/v1/sys/auth/**",
                        "/api/v1/rbac/auth/**",
                        "/api/v1/eq/gateway/report",
                        "/api/v1/pda/auth/login",
                        "/api/v1/pda/auth/scan-login",
                        "/api/v1/pda/config/version",
                        "/error"
                );
        registry.addInterceptor(new PermissionInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",
                        "/api/v1/sys/auth/**",
                        "/api/v1/rbac/auth/**",
                        "/api/v1/eq/gateway/report",
                        "/api/v1/pda/auth/login",
                        "/api/v1/pda/auth/scan-login",
                        "/api/v1/pda/config/version",
                        "/error"
                );
    }
}
