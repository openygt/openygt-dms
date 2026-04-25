package cn.org.openygt.system.aspect;

import cn.org.openygt.system.entity.SysLog;
import cn.org.openygt.system.service.SysLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.Mockito.*;

/**
 * OperationLogAspect 单元测试 —— 验证切面触发和异常场景。
 */
class OperationLogAspectTest {

    private TestController proxy;
    private SysLogService sysLogService;

    @BeforeEach
    void setUp() {
        sysLogService = mock(SysLogService.class);

        OperationLogAspect aspect = new OperationLogAspect(sysLogService);
        AspectJProxyFactory factory = new AspectJProxyFactory(new TestController());
        factory.addAspect(aspect);

        proxy = factory.getProxy();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test/success");
        request.setMethod("POST");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
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

    @RestController
    @RequestMapping("/test")
    static class TestController {
        @PostMapping("/success")
        public String success() {
            return "ok";
        }

        @PostMapping("/fail")
        public String fail() {
            throw new RuntimeException("模拟异常");
        }
    }
}
