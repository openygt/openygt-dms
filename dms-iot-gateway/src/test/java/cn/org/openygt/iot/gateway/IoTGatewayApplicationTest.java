package cn.org.openygt.iot.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "iot.gateway.api-key=test-key",
        "iot.gateway.lifecycle.auto-start-adapters=false"
})
public class IoTGatewayApplicationTest {

    @Test
    void contextLoads() {
        // Spring Boot 上下文加载测试
    }
}
