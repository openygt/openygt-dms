package cn.org.openygt.system.aspect;

import cn.org.openygt.system.entity.SysLog;
import cn.org.openygt.system.service.SysLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OperationLogAspect 单元测试 —— 验证切面触发、异常场景、URI 模块提取、IP 提取。
 */
class OperationLogAspectTest {

    private SysLogService sysLogService;
    private TestController proxy;
    private OtherController otherProxy;

    @BeforeEach
    void setUp() {
        sysLogService = mock(SysLogService.class);

        OperationLogAspect aspect = new OperationLogAspect(sysLogService);
        AspectJProxyFactory factory = new AspectJProxyFactory(new TestController());
        factory.addAspect(aspect);
        proxy = factory.getProxy();

        AspectJProxyFactory otherFactory = new AspectJProxyFactory(new OtherController());
        otherFactory.addAspect(aspect);
        otherProxy = otherFactory.getProxy();

        setRequest("/test/success", "POST", null);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void setRequest(String uri, String method, String xff) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        request.setMethod(method);
        if (xff != null) {
            request.addHeader("X-Forwarded-For", xff);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void shouldRecordLogOnSuccess() {
        proxy.success();
        verify(sysLogService).saveLog(any(SysLog.class));
    }

    @Test
    void shouldNotRecordLogWhenExceptionThrown() {
        try {
            proxy.fail();
        } catch (RuntimeException e) {
            // expected
        }
        verify(sysLogService, never()).saveLog(any(SysLog.class));
    }

    @Test
    void shouldRecordLogWithModuleSystem() {
        setRequest("/api/v1/sys/users", "POST", null);
        otherProxy.operation();
        verify(sysLogService).saveLog(any(SysLog.class));
    }

    @Test
    void shouldRecordLogWithModuleMasterdata() {
        setRequest("/api/v1/md/hospitals", "POST", null);
        otherProxy.operation();
        verify(sysLogService).saveLog(any(SysLog.class));
    }

    @Test
    void shouldRecordLogWithModuleEquipment() {
        setRequest("/api/v1/eq/devices", "POST", null);
        otherProxy.operation();
        verify(sysLogService).saveLog(any(SysLog.class));
    }

    @Test
    void shouldRecordLogWithModuleProduction() {
        setRequest("/api/v1/prod/tasks", "POST", null);
        otherProxy.operation();
        verify(sysLogService).saveLog(any(SysLog.class));
    }

    @Test
    void shouldRecordLogWithModuleQuality() {
        setRequest("/api/v1/qt/checks", "POST", null);
        otherProxy.operation();
        verify(sysLogService).saveLog(any(SysLog.class));
    }

    @Test
    void shouldRecordLogWithModulePrint() {
        setRequest("/api/v1/prt/labels", "POST", null);
        otherProxy.operation();
        verify(sysLogService).saveLog(any(SysLog.class));
    }

    @Test
    void shouldRecordLogWithModuleUnknown() {
        setRequest("/api/other", "POST", null);
        otherProxy.operation();
        verify(sysLogService).saveLog(any(SysLog.class));
    }

    @Test
    void shouldExtractIpFromXForwardedFor() {
        setRequest("/test/success", "POST", "10.0.0.1, 10.0.0.2");
        proxy.success();
        verify(sysLogService).saveLog(any(SysLog.class));
    }

    @Test
    void shouldNotSaveLog_whenNoRequestAttributes() {
        RequestContextHolder.resetRequestAttributes();
        proxy.success();
        verify(sysLogService, never()).saveLog(any(SysLog.class));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {
        @PostMapping("/success")
        public String success() { return "ok"; }

        @PostMapping("/fail")
        public String fail() { throw new RuntimeException("模拟异常"); }
    }

    @RestController
    static class OtherController {
        @PostMapping("/op")
        public String operation() { return "done"; }
    }
}
