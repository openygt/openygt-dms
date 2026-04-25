package cn.org.openygt.system.aspect;

import cn.org.openygt.system.entity.SysLog;
import cn.org.openygt.system.service.SysLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

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
            recordLog(joinPoint, elapsed);
        } catch (Exception e) {
            log.error("操作日志记录失败", e);
        }

        return result;
    }

    private void recordLog(ProceedingJoinPoint joinPoint, long elapsed) {
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

        // TODO: 从 JWT Token 中解析 userId
        sysLog.setUserId(null);

        sysLogService.saveLog(sysLog);
    }

    private String extractModule(String uri) {
        if (uri == null) return "unknown";
        if (uri.startsWith("/api/v1/sys")) return "system";
        if (uri.startsWith("/api/v1/md")) return "masterdata";
        if (uri.startsWith("/api/v1/eq")) return "equipment";
        if (uri.startsWith("/api/v1/prod")) return "production";
        if (uri.startsWith("/api/v1/qt")) return "quality";
        if (uri.startsWith("/api/v1/prt")) return "print";
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
