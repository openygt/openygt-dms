package cn.org.openygt.rbac.interceptor;

import cn.org.openygt.rbac.annotation.RequiresPermissions;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 权限拦截器。
 *
 * <p>检查方法上的 {@link RequiresPermissions} 注解，验证当前用户是否拥有所需角色之一。</p>
 * <p>无数据权限计算，仅做角色字符串匹配。</p>
 */
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        Method method = ((HandlerMethod) handler).getMethod();
        RequiresPermissions ann = method.getAnnotation(RequiresPermissions.class);
        if (ann == null) {
            return true;
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) request.getAttribute("roles");
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) request.getAttribute("permissions");

        boolean hasAny = (roles != null && !roles.isEmpty()) ||
                         (permissions != null && !permissions.isEmpty());
        if (!hasAny) {
            writeForbidden(response);
            return false;
        }

        for (String required : ann.value()) {
            if (roles != null && roles.contains(required)) {
                return true;
            }
            if (permissions != null && permissions.contains(required)) {
                return true;
            }
        }

        writeForbidden(response);
        return false;
    }

    private void writeForbidden(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"无操作权限\"}");
    }
}
