package cn.org.openygt.iot.config;

import cn.org.openygt.iot.gateway.security.GatewayAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GatewayWebConfig implements WebMvcConfigurer {

    private final GatewayAuthInterceptor gatewayAuthInterceptor;

    public GatewayWebConfig(GatewayAuthInterceptor gatewayAuthInterceptor) {
        this.gatewayAuthInterceptor = gatewayAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(gatewayAuthInterceptor)
                .addPathPatterns("/api/v1/iot/command");
    }
}
