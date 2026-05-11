package cn.org.openygt.iot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "iot.gateway")
public class GatewayProperties {

    private String name;
    private String backendUrl = "http://localhost:8080";
    private String apiKey;

    private final Retry retry = new Retry();
    private final Http http = new Http();
    private final Lifecycle lifecycle = new Lifecycle();
    private final DeviceSession deviceSession = new DeviceSession();
    private final Mqtt mqtt = new Mqtt();

    @Data
    public static class Mqtt {
        private String broker = "tcp://localhost:1883";
        private String clientId = "openygt-iot-gateway";
        private int qos = 1;
    }

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private long backoffMillis = 1000L;
    }

    @Data
    public static class Http {
        private int connectTimeoutMillis = 3000;
        private int readTimeoutMillis = 5000;
    }

    @Data
    public static class Lifecycle {
        private boolean autoStartAdapters = true;
    }

    @Data
    public static class DeviceSession {
        private long offlineTimeoutSeconds = 120L;
    }
}
