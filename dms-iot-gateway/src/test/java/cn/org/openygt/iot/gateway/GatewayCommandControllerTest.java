package cn.org.openygt.iot.gateway;

import cn.org.openygt.iot.adapter.AdapterRegistry;
import cn.org.openygt.iot.adapter.DeviceAdapter;
import cn.org.openygt.iot.config.GatewayProperties;
import cn.org.openygt.iot.gateway.security.GatewayAuthInterceptor;
import cn.org.openygt.iot.gateway.security.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GatewayCommandControllerTest {

    @Mock
    private AdapterRegistry registry;

    @Mock
    private DeviceAdapter adapter;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        GatewayProperties properties = new GatewayProperties();
        properties.setApiKey("test-key");
        GatewayAuthInterceptor interceptor = new GatewayAuthInterceptor(properties);

        GatewayCommandController controller = new GatewayCommandController(registry);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(interceptor)
                .addFilters(new TraceIdFilter())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void sendCommand_rejectsRequestWithoutApiKey() throws Exception {
        mockMvc.perform(post("/api/v1/iot/command")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    void sendCommand_allowsAuthorizedRequest() throws Exception {
        when(registry.getAdapter("penglin-mqtt")).thenReturn(adapter);
        doNothing().when(adapter).sendCommand(any(), any());

        mockMvc.perform(post("/api/v1/iot/command")
                        .header("X-Gateway-Api-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.success").value(true));
    }

    private String requestJson() throws Exception {
        GatewayCommandController.CommandRequest request = new GatewayCommandController.CommandRequest();
        request.setDeviceCode("DEV-001");
        request.setProtocolType("penglin-mqtt");
        request.setCommandType("START");
        request.setParams(Collections.singletonMap("mode", "AUTO"));
        return objectMapper.writeValueAsString(request);
    }
}
