package cn.org.openygt.iot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GatewayHttpClientConfig {

    @Bean
    public RestTemplate restTemplate(GatewayProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getHttp().getConnectTimeoutMillis());
        requestFactory.setReadTimeout(properties.getHttp().getReadTimeoutMillis());
        return new RestTemplate(requestFactory);
    }
}
