package cn.org.openygt.iot.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import cn.org.openygt.iot.config.GatewayProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(GatewayProperties.class)
@ComponentScan(basePackages = "cn.org.openygt.iot")
public class IoTGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(IoTGatewayApplication.class, args);
    }
}
