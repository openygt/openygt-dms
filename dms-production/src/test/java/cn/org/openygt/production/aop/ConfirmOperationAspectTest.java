package cn.org.openygt.production.aop;

import cn.org.openygt.production.annotation.ConfirmOperation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 关键操作二次确认 AOP 拦截器单元测试。
 */
class ConfirmOperationAspectTest {

    private ConfirmOperationAspect aspect;
    private ProceedingJoinPoint joinPoint;
    private ConfirmOperation annotation;

    @BeforeEach
    void setUp() {
        aspect = new ConfirmOperationAspect();
        joinPoint = mock(ProceedingJoinPoint.class);
        annotation = mock(ConfirmOperation.class);
    }

    @Test
    @DisplayName("已确认：Header X-Operation-Confirmed=true 放行")
    void testConfirmedHeaderAllows() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Operation-Confirmed", "true");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(annotation.value()).thenReturn("测试操作");
        when(annotation.message()).thenReturn("");
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.around(joinPoint, annotation);

        assertEquals("success", result);
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("未确认：无 Header 抛 IllegalStateException")
    void testMissingHeaderBlocks() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(annotation.value()).thenReturn("强制状态变更");
        when(annotation.message()).thenReturn("");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                aspect.around(joinPoint, annotation));

        assertTrue(ex.getMessage().contains("CONFIRM_REQUIRED"));
        assertTrue(ex.getMessage().contains("强制状态变更"));
    }

    @Test
    @DisplayName("未确认：Header 为 false 抛 IllegalStateException")
    void testFalseHeaderBlocks() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Operation-Confirmed", "false");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(annotation.value()).thenReturn("删除任务");
        when(annotation.message()).thenReturn("");

        assertThrows(IllegalStateException.class, () ->
                aspect.around(joinPoint, annotation));
    }

    @Test
    @DisplayName("自定义提示语：优先使用 annotation.message()")
    void testCustomMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(annotation.value()).thenReturn("操作");
        when(annotation.message()).thenReturn("自定义风险提示语");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                aspect.around(joinPoint, annotation));

        assertTrue(ex.getMessage().contains("自定义风险提示语"));
    }
}
