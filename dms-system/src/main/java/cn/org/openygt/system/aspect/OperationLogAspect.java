package cn.org.openygt.system.aspect;

import cn.org.openygt.system.entity.SysLog;
import cn.org.openygt.system.service.SysLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import cn.org.openygt.common.util.JwtUtil;
import org.springframework.util.StringUtils;

/**
 * 操作日志 AOP —— 拦截 Controller 层写操作，记录到 sys_log。
 *
 * <p>当前拦截所有 Controller 的 POST/PUT/DELETE 方法。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysLogService sysLogService;

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerLayer() {
    }

    @Around("controllerLayer() && (@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public Object aroundWriteOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long elapsed = System.currentTimeMillis() - start;

        try {
            recordLog(joinPoint, elapsed, true, result);
        } catch (Exception e) {
            log.error("操作日志记录失败", e);
        }

        return result;
    }

    @AfterThrowing(pointcut = "controllerLayer() && (@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping))", throwing = "ex")
    public void afterThrowingOperation(org.aspectj.lang.JoinPoint joinPoint, Throwable ex) {
        try {
            recordLog(joinPoint, 0, false, null);
        } catch (Exception e) {
            log.error("操作日志记录失败", e);
        }
    }

    private void recordLog(org.aspectj.lang.JoinPoint joinPoint, long elapsed, boolean success, Object returnValue) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String action = className + "." + methodName;

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }
        HttpServletRequest request = attrs.getRequest();

        SysLog sysLog = new SysLog();
        sysLog.setAction(action);
        sysLog.setModule(extractModule(request.getRequestURI()));
        sysLog.setDetail(request.getMethod() + " " + request.getRequestURI() + " | 耗时=" + elapsed + "ms");
        sysLog.setIpAddress(getClientIp(request));
        sysLog.setCreatedAt(LocalDateTime.now());
        sysLog.setResult(success ? "SUCCESS" : "FAILED");

        // 从 JWT Token 中解析 userId
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                Long userId = cn.org.openygt.common.util.JwtUtil.getUserId(token);
                if (userId != null) sysLog.setUserId(String.valueOf(userId));
            } catch (Exception e) { /* token 无效，回退到请求参数 */ }
        }
        // 登录接口无 JWT，从请求体中提取 username 作为操作人
        if (sysLog.getUserId() == null && action.endsWith(".login")) {
            sysLog.setUserId(extractLoginUsername(joinPoint.getArgs()));
        }

        // 尝试从参数中提取 targetId / targetName
        Object[] args = joinPoint.getArgs();
        String targetId = extractTargetId(args);
        if (targetId != null) sysLog.setTargetId(targetId);

        sysLogService.saveLog(sysLog);
    }

    private String extractTargetId(Object[] args) {
        for (Object arg : args) {
            if (arg == null) continue;
            if (arg instanceof Number) {
                return arg.toString();
            }
            if (arg instanceof String) {
                String s = (String) arg;
                if (s.matches("\\d+")) return s;
            }
            try {
                java.lang.reflect.Method m = arg.getClass().getMethod("getId");
                Object id = m.invoke(arg);
                if (id != null) return id.toString();
            } catch (Exception e) { log.warn("操作日志记录失败", e); }
        }
        return null;
    }

    /** 从登录请求参数中提取 username，用于无 JWT 时记录操作人。 */
    private String extractLoginUsername(Object[] args) {
        for (Object arg : args) {
            if (arg == null) continue;
            try {
                java.lang.reflect.Method m = arg.getClass().getMethod("getUsername");
                Object username = m.invoke(arg);
                if (username != null) return username.toString();
            } catch (Exception e) { /* ignore */ }
        }
        return null;
    }

    private String extractModule(String uri) {
        if (uri == null) return "unknown";
        if (uri.startsWith("/api/v1/auth")) return "auth";
        if (uri.startsWith("/api/v1/rbac")) return "auth";
        if (uri.startsWith("/api/v1/sys")) return "system";
        if (uri.startsWith("/api/v1/md")) return "masterdata";
        if (uri.startsWith("/api/v1/eq")) return "equipment";
        if (uri.startsWith("/api/v1/prod")) return "production";
        if (uri.startsWith("/api/v1/qt")) return "quality";
        if (uri.startsWith("/api/v1/prt")) return "print";
        if (uri.startsWith("/api/v1/inv")) return "inventory";
        if (uri.startsWith("/api/v1/analytics")) return "analytics";
        if (uri.startsWith("/api/v1/ops")) return "analytics";
        return "unknown";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
