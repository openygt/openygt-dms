package cn.org.openygt.equipment.scheduler;

import cn.org.openygt.common.service.SysConfigService;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import static org.mockito.Mockito.*;

/**
 * HeartbeatCheckScheduler 心跳超时检测测试。
 *
 * <p>覆盖场景：
 * <ul>
 *   <li>IDLE 状态设备超时（>120s）→ OFFLINE</li>
 *   <li>RUNNING 状态设备超时（>600s）→ OFFLINE</li>
 *   <li>IDLE 状态设备心跳未超时（<120s）→ 保持在线</li>
 *   <li>RUNNING 状态设备未超时（<600s）→ 保持在线</li>
 *   <li>从未上报过心跳的设备 → OFFLINE</li>
 *   <li>无活跃设备 → 不执行任何更新</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class HeartbeatCheckSchedulerTest {

    @Mock
    private EqDeviceMapper eqDeviceMapper;

    @Mock
    private SysConfigService sysConfigService;

    @Captor
    private ArgumentCaptor<EqDevice> deviceCaptor;

    private HeartbeatCheckScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new HeartbeatCheckScheduler(eqDeviceMapper, sysConfigService);
    }

    @Test
    @DisplayName("IDLE 设备超时（>120s）→ OFFLINE")
    void testIdleDevice_timeout_marksOffline() {
        // Arrange
        EqDevice idleDevice = new EqDevice();
        idleDevice.setId(1L);
        idleDevice.setDeviceCode("D001");
        idleDevice.setStatus("IDLE");
        idleDevice.setLastHeartbeat(LocalDateTime.now().minusSeconds(180)); // 180s > 120s

        when(sysConfigService.getIntValue(anyString(), anyInt())).thenReturn(120, 600);
        when(eqDeviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(idleDevice));

        // Act
        scheduler.checkHeartbeatTimeout();

        // Assert
        verify(eqDeviceMapper).updateById(deviceCaptor.capture());
        EqDevice updated = deviceCaptor.getValue();
        assertThat(updated.getStatus()).isEqualTo("OFFLINE");
        assertThat(updated.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("RUNNING 设备超时（>600s）→ OFFLINE")
    void testRunningDevice_timeout_marksOffline() {
        // Arrange
        EqDevice runningDevice = new EqDevice();
        runningDevice.setId(2L);
        runningDevice.setDeviceCode("D002");
        runningDevice.setStatus("RUNNING");
        runningDevice.setLastHeartbeat(LocalDateTime.now().minusSeconds(900)); // 900s > 600s

        when(sysConfigService.getIntValue(anyString(), anyInt())).thenReturn(120, 600);
        when(eqDeviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(runningDevice));

        // Act
        scheduler.checkHeartbeatTimeout();

        // Assert
        verify(eqDeviceMapper).updateById(deviceCaptor.capture());
        assertThat(deviceCaptor.getValue().getStatus()).isEqualTo("OFFLINE");
    }

    @Test
    @DisplayName("IDLE 设备未超时（<120s）→ 保持在线")
    void testIdleDevice_noTimeout_staysOnline() {
        // Arrange
        EqDevice idleDevice = new EqDevice();
        idleDevice.setId(1L);
        idleDevice.setDeviceCode("D001");
        idleDevice.setStatus("IDLE");
        idleDevice.setLastHeartbeat(LocalDateTime.now().minusSeconds(60)); // 60s < 120s

        when(sysConfigService.getIntValue(anyString(), anyInt())).thenReturn(120, 600);
        when(eqDeviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(idleDevice));

        // Act
        scheduler.checkHeartbeatTimeout();

        // Assert
        verify(eqDeviceMapper, never()).updateById(any(EqDevice.class));
    }

    @Test
    @DisplayName("RUNNING 设备未超时（<600s）→ 保持在线")
    void testRunningDevice_noTimeout_staysOnline() {
        // Arrange
        EqDevice runningDevice = new EqDevice();
        runningDevice.setId(2L);
        runningDevice.setDeviceCode("D002");
        runningDevice.setStatus("RUNNING");
        runningDevice.setLastHeartbeat(LocalDateTime.now().minusSeconds(300)); // 300s < 600s

        when(sysConfigService.getIntValue(anyString(), anyInt())).thenReturn(120, 600);
        when(eqDeviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(runningDevice));

        // Act
        scheduler.checkHeartbeatTimeout();

        // Assert
        verify(eqDeviceMapper, never()).updateById(any(EqDevice.class));
    }

    @Test
    @DisplayName("从未上报心跳 → OFFLINE")
    void testNullHeartbeat_marksOffline() {
        // Arrange
        EqDevice newDevice = new EqDevice();
        newDevice.setId(3L);
        newDevice.setDeviceCode("D003");
        newDevice.setStatus("IDLE");
        newDevice.setLastHeartbeat(null);

        when(sysConfigService.getIntValue(anyString(), anyInt())).thenReturn(120, 600);
        when(eqDeviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(newDevice));

        // Act
        scheduler.checkHeartbeatTimeout();

        // Assert
        verify(eqDeviceMapper).updateById(deviceCaptor.capture());
        assertThat(deviceCaptor.getValue().getStatus()).isEqualTo("OFFLINE");
    }

    @Test
    @DisplayName("无活跃设备 → 不执行更新")
    void testNoActiveDevices_noUpdate() {
        when(sysConfigService.getIntValue(anyString(), anyInt())).thenReturn(120, 600);
        when(eqDeviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        scheduler.checkHeartbeatTimeout();

        verify(eqDeviceMapper, never()).updateById(any(EqDevice.class));
    }

    @Test
    @DisplayName("多台设备混合同步检测：只有超时的变为 OFFLINE")
    void testMixedDevices_onlyTimeoutGoOffline() {
        EqDevice idleTimedOut = new EqDevice();
        idleTimedOut.setId(1L);
        idleTimedOut.setDeviceCode("D001");
        idleTimedOut.setStatus("IDLE");
        idleTimedOut.setLastHeartbeat(LocalDateTime.now().minusSeconds(180)); // 超时

        EqDevice runningOk = new EqDevice();
        runningOk.setId(2L);
        runningOk.setDeviceCode("D002");
        runningOk.setStatus("RUNNING");
        runningOk.setLastHeartbeat(LocalDateTime.now().minusSeconds(300)); // 未超时

        when(sysConfigService.getIntValue(anyString(), anyInt())).thenReturn(120, 600);
        when(eqDeviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(idleTimedOut, runningOk));

        scheduler.checkHeartbeatTimeout();

        // 只有 1 号设备被更新为 OFFLINE
        verify(eqDeviceMapper, times(1)).updateById(deviceCaptor.capture());
        assertThat(deviceCaptor.getValue().getStatus()).isEqualTo("OFFLINE");
        assertThat(deviceCaptor.getValue().getId()).isEqualTo(1L);
    }
}
