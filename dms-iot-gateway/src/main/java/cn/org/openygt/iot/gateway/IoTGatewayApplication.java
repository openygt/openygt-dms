package cn.org.openygt.iot.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "cn.org.openygt.iot")
public class IoTGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(IoTGatewayApplication.class, args);
    }
}
