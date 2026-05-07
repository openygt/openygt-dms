package cn.org.openygt.rbac.interceptor;

import cn.org.openygt.rbac.annotation.RequiresPermissions;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限拦截器。
 *
 * <p>检查方法上的 {@link RequiresPermissions} 注解，验证当前用户是否拥有所需角色之一。</p>
 * <p>无数据权限计算，仅做角色字符串匹配。</p>
 */
public class PermissionInterceptor implements HandlerInterceptor {

    /**
     * 权限码兼容映射：新权限码 → 旧权限码
     * <p>第一批治理期间，后端同时接受新旧权限码，确保切换期间不中断。</p>
     */
    private static final Map<String, String> PERMISSION_COMPAT_MAP = new HashMap<>();
    static {
        PERMISSION_COMPAT_MAP.put("eq:device:view", "eq:device:list");
        PERMISSION_COMPAT_MAP.put("eq:device:view:mine", "eq:device:list:mine");
        PERMISSION_COMPAT_MAP.put("prod:prescription:view", "prod:prescription:list");
        PERMISSION_COMPAT_MAP.put("qt:inspect:view", "qt:inspect:list");
        PERMISSION_COMPAT_MAP.put("sys:user:view", "sys:user:list");
        PERMISSION_COMPAT_MAP.put("sys:role:view", "sys:role:list");
        PERMISSION_COMPAT_MAP.put("sys:menu:view", "sys:menu:list");
        PERMISSION_COMPAT_MAP.put("sys:config:view", "sys:config:list");
        PERMISSION_COMPAT_MAP.put("sys:log:view", "sys:log:list");
        PERMISSION_COMPAT_MAP.put("inv:log:view", "inv:log:list");
    }

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

        // 管理员/主任拥有全部权限，直接放行
        boolean adminLike = (roles != null && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_DIRECTOR")))
                || (permissions != null && (permissions.contains("ROLE_ADMIN") || permissions.contains("ROLE_DIRECTOR")));
        if (adminLike) {
            return true;
        }

        for (String required : ann.value()) {
            if (roles != null && roles.contains(required)) {
                return true;
            }
            if (permissions != null && permissions.contains(required)) {
                return true;
            }
            // 兼容旧权限码：如果用户持有旧码，也放行
            String oldPerm = PERMISSION_COMPAT_MAP.get(required);
            if (oldPerm != null) {
                if (roles != null && roles.contains(oldPerm)) {
                    return true;
                }
                if (permissions != null && permissions.contains(oldPerm)) {
                    return true;
                }
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
