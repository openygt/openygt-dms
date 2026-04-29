package cn.org.openygt.common.interceptor;

import cn.org.openygt.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * JWT 认证拦截器。
 *
 * <p>拦截所有请求，验证 Authorization Header 中的 Bearer Token。</p>
 * <p>白名单路径（如登录接口）应在 {@link cn.org.openygt.config.WebMvcConfig} 中排除。</p>
 */
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 公开接口放行（兜底，实际白名单在 WebMvcConfig 中配置）
        if (uri.startsWith("/api/v1/auth/") || uri.startsWith("/api/v1/sys/auth/") || uri.startsWith("/error")) {
            return true;
        }

        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("未提供有效的 Authorization Header: {}", uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未认证，请登录\"}");
            return false;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!JwtUtil.validateToken(token)) {
            log.warn("Token 无效或已过期: {}", uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 无效或已过期\"}");
            return false;
        }

        // 将用户信息放入请求属性，供后续使用
        Long userId = JwtUtil.getUserId(token);
        String username = JwtUtil.getUsername(token);
        List<String> roles = JwtUtil.getRoles(token);
        List<String> permissions = JwtUtil.getPermissions(token);
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("roles", roles);
        request.setAttribute("permissions", permissions);

        return true;
    }
}
