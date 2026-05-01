package cn.org.openygt.production.aop;

import cn.org.openygt.production.annotation.ConfirmOperation;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 关键操作二次确认拦截器。
 * <p>拦截 {@link ConfirmOperation} 注解，校验前端是否已确认。</p>
 */
@Slf4j
@Aspect
@Component
public class ConfirmOperationAspect {

    private static final String CONFIRM_HEADER = "X-Operation-Confirmed";

    @Around("@annotation(confirmOperation)")
    public Object around(ProceedingJoinPoint point, ConfirmOperation confirmOperation) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String confirmed = request.getHeader(CONFIRM_HEADER);
            if (!"true".equalsIgnoreCase(confirmed)) {
                String message = confirmOperation.message();
                if (message.isEmpty()) {
                    message = "【" + confirmOperation.value() + "】为高风险操作，请二次确认后重试（Header: X-Operation-Confirmed=true）";
                }
                log.warn("二次确认拦截: {} - {}", confirmOperation.value(), request.getRequestURI());
                throw new IllegalStateException("CONFIRM_REQUIRED: " + message);
            }
        }
        return point.proceed();
    }
}
