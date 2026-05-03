package cn.org.openygt.iot.gateway.security;

import cn.org.openygt.iot.config.GatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class GatewayAuthInterceptor implements HandlerInterceptor {

    private static final String HEADER_NAME = "X-Gateway-Api-Key";

    private final GatewayProperties properties;

    public GatewayAuthInterceptor(GatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String expected = properties.getApiKey();
        if (expected == null || expected.isEmpty()) {
            log.error("网关 API Key 未配置，拒绝指令请求");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Gateway API key is not configured");
            return false;
        }

        String actual = request.getHeader(HEADER_NAME);
        if (!expected.equals(actual)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return false;
        }

        return true;
    }
}
