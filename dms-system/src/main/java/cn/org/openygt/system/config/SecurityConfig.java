package cn.org.openygt.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.servlet.http.HttpServletResponse;

/**
 * 安全配置。
 *
 * <p>注册 BCryptPasswordEncoder Bean，并配置最小可用的 SecurityFilterChain。</p>
 * <p>实际认证授权由 MVC {@link cn.org.openygt.common.interceptor.AuthInterceptor} 与
 * {@link cn.org.openygt.rbac.interceptor.PermissionInterceptor} 兜底。</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors().and()
            .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"Unauthorized\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (request.getUserPrincipal() == null || "anonymousUser".equals(request.getUserPrincipal().getName())) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":401,\"message\":\"Unauthorized\"}");
                    } else {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":403,\"message\":\"Forbidden\"}");
                    }
                })
            .and()
            .authorizeRequests()
                .antMatchers("/api/v1/auth/**", "/api/v1/sys/auth/**", "/api/v1/rbac/auth/**",
                             "/error", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            .and()
            .httpBasic().disable()
            .formLogin().disable();
        return http.build();
    }
}
