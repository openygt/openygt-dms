package cn.org.openygt.rbac.interceptor;

import cn.org.openygt.rbac.annotation.RequiresPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PermissionInterceptorTest {

    private PermissionInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new PermissionInterceptor();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @RequiresPermissions({"ROLE_ADMIN", "ROLE_DIRECTOR"})
    public void adminOrDirectorMethod() {
    }

    @RequiresPermissions({"ROLE_WORKER"})
    public void workerOnlyMethod() {
    }

    public void noAnnotationMethod() {
    }

    @Test
    void shouldPass_whenNoAnnotation() throws Exception {
        HandlerMethod handler = new HandlerMethod(this, "noAnnotationMethod");
        assertTrue(interceptor.preHandle(request, response, handler));
    }

    @Test
    void shouldPass_whenRoleMatched() throws Exception {
        request.setAttribute("roles", Collections.singletonList("ROLE_ADMIN"));
        HandlerMethod handler = new HandlerMethod(this, "adminOrDirectorMethod");
        assertTrue(interceptor.preHandle(request, response, handler));
    }

    @Test
    void shouldPass_whenAnyRoleMatched() throws Exception {
        request.setAttribute("roles", Arrays.asList("ROLE_WORKER", "ROLE_DIRECTOR"));
        HandlerMethod handler = new HandlerMethod(this, "adminOrDirectorMethod");
        assertTrue(interceptor.preHandle(request, response, handler));
    }

    @Test
    void shouldReject_whenRoleNotMatched() throws Exception {
        request.setAttribute("roles", Collections.singletonList("ROLE_WORKER"));
        HandlerMethod handler = new HandlerMethod(this, "adminOrDirectorMethod");
        assertFalse(interceptor.preHandle(request, response, handler));
        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldReject_whenNoRolesInRequest() throws Exception {
        HandlerMethod handler = new HandlerMethod(this, "workerOnlyMethod");
        assertFalse(interceptor.preHandle(request, response, handler));
        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldReject_whenEmptyRoles() throws Exception {
        request.setAttribute("roles", Collections.emptyList());
        HandlerMethod handler = new HandlerMethod(this, "workerOnlyMethod");
        assertFalse(interceptor.preHandle(request, response, handler));
        assertEquals(403, response.getStatus());
    }
}
