package cn.org.openygt.iot.router;

import cn.org.openygt.iot.config.GatewayProperties;
import cn.org.openygt.iot.gateway.session.GatewayDeviceSessionController;
import cn.org.openygt.iot.protocol.DeviceMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageRouterTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private GatewayDeviceSessionController gatewayDeviceSessionController;

    private GatewayProperties properties;
    private MessageRouter router;

    @BeforeEach
    void setUp() {
        properties = new GatewayProperties();
        properties.setBackendUrl("http://equipment-service");
        properties.getRetry().setMaxAttempts(3);
        properties.getRetry().setBackoffMillis(0L);
        router = new MessageRouter(restTemplate, properties, gatewayDeviceSessionController);
    }

    @Test
    void route_retriesBeforeSuccess() {
        DeviceMessage message = buildMessage("telemetry");
        when(restTemplate.postForEntity(eq("http://equipment-service/api/v1/eq/gateway/report"), any(), eq(Object.class)))
                .thenThrow(new RuntimeException("timeout"))
                .thenReturn(ResponseEntity.ok().build());

        router.route(message);

        verify(restTemplate, times(2))
                .postForEntity(eq("http://equipment-service/api/v1/eq/gateway/report"), any(), eq(Object.class));
    }

    @Test
    void route_stopsAfterMaxAttempts() {
        DeviceMessage message = buildMessage("STATUS");
        when(restTemplate.postForEntity(eq("http://equipment-service/api/v1/eq/gateway/report"), any(), eq(Object.class)))
                .thenThrow(new RuntimeException("down"));

        router.route(message);

        verify(restTemplate, times(3))
                .postForEntity(eq("http://equipment-service/api/v1/eq/gateway/report"), any(), eq(Object.class));
    }

    @Test
    void route_ignoresUnknownMessageType() {
        DeviceMessage message = buildMessage("UNKNOWN_TYPE");

        router.route(message);

        verify(restTemplate, never()).postForEntity(any(String.class), any(), eq(Object.class));
    }

    private DeviceMessage buildMessage(String messageType) {
        DeviceMessage message = new DeviceMessage();
        message.setDeviceCode("DEV-001");
        message.setProtocolType("penglin-mqtt");
        message.setMessageType(messageType);
        message.setPayload(Collections.singletonMap("status", "RUNNING"));
        return message;
    }
}
