package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.equipment.entity.DeviceCommand;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.gateway.IoTGatewayClient;
import cn.org.openygt.equipment.mapper.DeviceCommandMapper;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import cn.org.openygt.equipment.websocket.DeviceWebSocketController;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeviceCommandServiceImplTest {

    @Mock
    private DeviceCommandMapper commandMapper;
    @Mock
    private EqDeviceMapper deviceMapper;
    @Mock
    private EquipmentService equipmentService;
    @Mock
    private DeviceWebSocketController webSocketController;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private IoTGatewayClient iotGatewayClient;

    @Captor
    private ArgumentCaptor<QueryWrapper<EqDevice>> deviceWrapperCaptor;

    private DeviceCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeviceCommandServiceImpl(
                commandMapper, deviceMapper, equipmentService,
                webSocketController, objectMapper, iotGatewayClient
        );
    }

    private EqDevice mockDevice(String detailStatus, String status) {
        EqDevice device = new EqDevice();
        device.setId(1L);
        device.setDeviceCode("DECOCT_001");
        device.setDetailStatus(detailStatus);
        device.setStatus(status);
        device.setProtocolType("penglin-mqtt");
        device.setTenantId("default");
        return device;
    }

    @Nested
    @DisplayName("P0-3: 设备状态校验")
    public class DeviceStatusValidationTests {

        @Test
        @DisplayName("规则冲突: 离线设备禁止发送任何操控指令")
        void createCommand_offlineDevice_shouldThrow() {
            when(deviceMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(mockDevice("OFFLINE", "OFFLINE"));

            assertThatThrownBy(() -> service.createCommand("DECOCT_001", "START_SOAK", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("离线状态");

            verify(deviceMapper).selectOne(any(QueryWrapper.class));
            verify(commandMapper, never()).insert((DeviceCommand) any());
        }

        @Test
        @DisplayName("规则冲突: 故障设备禁止发送启动类指令")
        void createCommand_faultDeviceWithStartCommand_shouldThrow() {
            when(deviceMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(mockDevice("FAULT", "FAULT"));

            assertThatThrownBy(() -> service.createCommand("DECOCT_001", "START_SOAK", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("故障状态");

            verify(commandMapper, never()).insert((DeviceCommand) any());
        }

        @Test
        @DisplayName("正常场景: 故障设备允许发送停止类指令")
        void createCommand_faultDeviceWithStopCommand_shouldSucceed() {
            when(deviceMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(mockDevice("FAULT", "FAULT"));
            when(equipmentService.getDeviceId("DECOCT_001")).thenReturn(1L);
            when(iotGatewayClient.sendCommand(any(), any(), any(), any())).thenReturn(true);
            doNothing().when(equipmentService).updateDeviceStatus(any(), any());

            DeviceCommand result = service.createCommand("DECOCT_001", "STOP", null);

            assertThat(result).isNotNull();
            verify(commandMapper).insert((DeviceCommand) any());
        }

        @Test
        @DisplayName("正常场景: 空闲设备允许发送启动类指令")
        void createCommand_idleDeviceWithStartCommand_shouldSucceed() {
            EqDevice device = mockDevice("IDLE", "IDLE");
            when(deviceMapper.selectOne(any(QueryWrapper.class))).thenReturn(device);
            when(equipmentService.getDeviceId("DECOCT_001")).thenReturn(1L);
            when(iotGatewayClient.sendCommand(any(), any(), any(), any())).thenReturn(true);
            doNothing().when(equipmentService).updateDeviceStatus(any(), any());

            DeviceCommand result = service.createCommand("DECOCT_001", "START_SOAK", null);

            assertThat(result).isNotNull();
            assertThat(result.getCommandType()).isEqualTo("START_SOAK");
            verify(commandMapper).insert((DeviceCommand) any());
        }

        @Test
        @DisplayName("规则冲突: 设备不存在时应拦截")
        void createCommand_deviceNotFound_shouldThrow() {
            when(deviceMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

            assertThatThrownBy(() -> service.createCommand("DECOCT_999", "START_SOAK", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("设备不存在");

            verify(commandMapper, never()).insert((DeviceCommand) any());
        }
    }
}
