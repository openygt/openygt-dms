package cn.org.openygt.equipment.config;

import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.equipment.dto.DeviceStatusPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * MqttConfig JSON 解析测试。
 *
 * <p>覆盖场景：
 * <ul>
 *   <li>正常 JSON 解析（温度 + 状态 + 故障码）</li>
 *   <li>非法 JSON 回退到旧格式 BigDecimal 解析</li>
 *   <li>完全不可解析的 payload</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class MqttConfigTest {

    @Mock
    private EquipmentService equipmentService;

    private MqttConfig mqttConfig;
    private ObjectMapper objectMapper;
    private Method handleStatusMessageMethod;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        mqttConfig = new MqttConfig(equipmentService, objectMapper);

        // 反射获取私有方法 handleStatusMessage(Long deviceId, String payload)
        handleStatusMessageMethod = MqttConfig.class.getDeclaredMethod("handleStatusMessage", Long.class, String.class);
        handleStatusMessageMethod.setAccessible(true);
    }

    @Test
    @DisplayName("正常 JSON 解析：包含温度和状态")
    void testValidJson_parsesSuccessfully() throws Exception {
        String payload = "{\"deviceCode\":\"D001\",\"status\":\"RUNNING\",\"temperature\":98.5}";

        handleStatusMessageMethod.invoke(mqttConfig, 1L, payload);

        verify(equipmentService).updateTemperature(1L, new BigDecimal("98.5"));
        verify(equipmentService).updateDeviceStatus(1L, "RUNNING");
    }

    @Test
    @DisplayName("正常 JSON 解析：包含故障码")
    void testValidJson_withFaultCode() throws Exception {
        String payload = "{\"deviceCode\":\"D001\",\"status\":\"FAULT\",\"temperature\":120.0,\"faultCode\":\"E001\"}";

        handleStatusMessageMethod.invoke(mqttConfig, 1L, payload);

        verify(equipmentService, never()).updateDeviceStatus(anyLong(), any());
        verify(equipmentService).reportFault(1L, "E001", "状态上报故障");
    }

    @Test
    @DisplayName("非法 JSON 回退：解析为纯温度值")
    void testInvalidJson_fallsBackToBigDecimal() throws Exception {
        String payload = "98.5";

        handleStatusMessageMethod.invoke(mqttConfig, 1L, payload);

        verify(equipmentService).updateTemperature(1L, new BigDecimal("98.5"));
    }

    @Test
    @DisplayName("完全不可解析的 payload，不抛出异常")
    void testGarbagePayload_doesNotThrow() throws Exception {
        String payload = "not a number or json";

        handleStatusMessageMethod.invoke(mqttConfig, 1L, payload);

        verify(equipmentService, never()).updateTemperature(anyLong(), any());
        verify(equipmentService, never()).reportFault(anyLong(), any(), any());
    }

    @Test
    @DisplayName("ObjectMapper 正确反序列化 DeviceStatusPayload")
    void testObjectMapper_deserializesCorrectly() throws Exception {
        String payload = "{\"deviceCode\":\"D001\",\"status\":\"RUNNING\",\"temperature\":98.5,\"faultCode\":null}";

        DeviceStatusPayload result = objectMapper.readValue(payload, DeviceStatusPayload.class);

        assertThat(result.getDeviceCode()).isEqualTo("D001");
        assertThat(result.getStatus()).isEqualTo("RUNNING");
        assertThat(result.getTemperature()).isEqualByComparingTo(new BigDecimal("98.5"));
        assertThat(result.getFaultCode()).isNull();
    }
}
