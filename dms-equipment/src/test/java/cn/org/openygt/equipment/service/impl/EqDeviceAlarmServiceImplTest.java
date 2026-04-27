package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.equipment.entity.EqAlarmNotification;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.entity.EqDeviceAlarm;
import cn.org.openygt.equipment.mapper.EqAlarmNotificationMapper;
import cn.org.openygt.equipment.mapper.EqDeviceAlarmMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EqDeviceAlarmServiceImpl 单元测试。
 *
 * <p>覆盖场景：
 * <ul>
 *   <li>温度告警触发与 IN_APP 站内通知写入</li>
 *   <li>告警冷却期去重</li>
 *   <li>温度正常自动消警</li>
 *   <li>离线告警创建</li>
 *   <li>VOICE 类型通知被过滤为 IN_APP</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class EqDeviceAlarmServiceImplTest {

    @Mock
    private EqDeviceAlarmMapper alarmMapper;

    @Mock
    private EqAlarmNotificationMapper notificationMapper;

    @Mock
    private EquipmentService equipmentService;

    @Captor
    private ArgumentCaptor<EqDeviceAlarm> alarmCaptor;

    @Captor
    private ArgumentCaptor<EqAlarmNotification> notificationCaptor;

    private EqDeviceAlarmServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EqDeviceAlarmServiceImpl(alarmMapper, notificationMapper, equipmentService);
    }

    @Nested
    @DisplayName("温度告警触发")
    class TemperatureAlarm {

        @Test
        @DisplayName("高温告警触发：创建告警记录与 IN_APP 站内通知")
        void testHighTemp_createsAlarmAndInAppNotification() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");

            cn.org.openygt.common.dto.TemperatureThresholdDTO threshold =
                    new cn.org.openygt.common.dto.TemperatureThresholdDTO();
            threshold.setHighTemp(new BigDecimal("100.0"));
            threshold.setLowTemp(new BigDecimal("80.0"));
            threshold.setSource("DEVICE");

            when(equipmentService.getEffectiveThreshold(1L)).thenReturn(threshold);
            when(alarmMapper.findLatestActiveAlarm(1L, "HIGH_TEMP")).thenReturn(null);
            when(alarmMapper.insert(any(EqDeviceAlarm.class))).thenAnswer(inv -> {
                EqDeviceAlarm a = inv.getArgument(0);
                a.setId(100L);
                return 1;
            });

            service.checkTemperatureAlarm(device, new BigDecimal("105.0"));

            verify(alarmMapper).insert(alarmCaptor.capture());
            EqDeviceAlarm alarm = alarmCaptor.getValue();
            assertThat(alarm.getAlarmType()).isEqualTo("HIGH_TEMP");
            assertThat(alarm.getAlarmLevel()).isEqualTo("CRITICAL");
            assertThat(alarm.getIsResolved()).isEqualTo(0);

            verify(notificationMapper).insert(notificationCaptor.capture());
            EqAlarmNotification notification = notificationCaptor.getValue();
            assertThat(notification.getNotifyType()).isEqualTo("IN_APP");
            assertThat(notification.getAlarmId()).isEqualTo(100L);
            assertThat(notification.getSendStatus()).isEqualTo("SENT");
        }

        @Test
        @DisplayName("低温告警触发：创建 LOW_TEMP 告警")
        void testLowTemp_createsLowTempAlarm() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");

            cn.org.openygt.common.dto.TemperatureThresholdDTO threshold =
                    new cn.org.openygt.common.dto.TemperatureThresholdDTO();
            threshold.setHighTemp(new BigDecimal("100.0"));
            threshold.setLowTemp(new BigDecimal("80.0"));
            threshold.setSource("DEVICE");

            when(equipmentService.getEffectiveThreshold(1L)).thenReturn(threshold);
            when(alarmMapper.findLatestActiveAlarm(1L, "LOW_TEMP")).thenReturn(null);
            when(alarmMapper.insert(any(EqDeviceAlarm.class))).thenAnswer(inv -> {
                EqDeviceAlarm a = inv.getArgument(0);
                a.setId(101L);
                return 1;
            });

            service.checkTemperatureAlarm(device, new BigDecimal("75.0"));

            verify(alarmMapper).insert(alarmCaptor.capture());
            assertThat(alarmCaptor.getValue().getAlarmType()).isEqualTo("LOW_TEMP");
        }

        @Test
        @DisplayName("温度正常时自动消警：解除 HIGH_TEMP 和 LOW_TEMP")
        void testTempNormal_autoResolvesAlarms() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");

            cn.org.openygt.common.dto.TemperatureThresholdDTO threshold =
                    new cn.org.openygt.common.dto.TemperatureThresholdDTO();
            threshold.setHighTemp(new BigDecimal("100.0"));
            threshold.setLowTemp(new BigDecimal("80.0"));
            threshold.setSource("DEVICE");

            when(equipmentService.getEffectiveThreshold(1L)).thenReturn(threshold);
            when(alarmMapper.findActiveByDeviceIdAndType(1L, "HIGH_TEMP"))
                    .thenReturn(Collections.emptyList());
            when(alarmMapper.findActiveByDeviceIdAndType(1L, "LOW_TEMP"))
                    .thenReturn(Collections.emptyList());

            service.checkTemperatureAlarm(device, new BigDecimal("90.0"));

            verify(alarmMapper).findActiveByDeviceIdAndType(1L, "HIGH_TEMP");
            verify(alarmMapper).findActiveByDeviceIdAndType(1L, "LOW_TEMP");
        }
    }

    @Nested
    @DisplayName("告警防抖与去重")
    class Debounce {

        @Test
        @DisplayName("5 分钟内重复同类告警被抑制")
        void testDebounce_suppressesRepeatAlarm() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");

            EqDeviceAlarm recentAlarm = new EqDeviceAlarm();
            recentAlarm.setId(99L);
            recentAlarm.setCreatedAt(LocalDateTime.now().minusMinutes(2));

            when(alarmMapper.findLatestActiveAlarm(1L, "HIGH_TEMP")).thenReturn(recentAlarm);

            service.createAlarm(device, "HIGH_TEMP", "CRITICAL", "超温");

            verify(alarmMapper, never()).insert(any(EqDeviceAlarm.class));
            verify(notificationMapper, never()).insert(any(EqAlarmNotification.class));
        }

        @Test
        @DisplayName("超过 5 分钟后允许再次触发")
        void testAfterCooldown_allowsNewAlarm() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");

            EqDeviceAlarm oldAlarm = new EqDeviceAlarm();
            oldAlarm.setId(99L);
            oldAlarm.setCreatedAt(LocalDateTime.now().minusMinutes(10));

            when(alarmMapper.findLatestActiveAlarm(1L, "HIGH_TEMP")).thenReturn(oldAlarm);
            when(alarmMapper.insert(any(EqDeviceAlarm.class))).thenAnswer(inv -> {
                EqDeviceAlarm a = inv.getArgument(0);
                a.setId(100L);
                return 1;
            });

            service.createAlarm(device, "HIGH_TEMP", "CRITICAL", "超温");

            verify(alarmMapper).insert(any(EqDeviceAlarm.class));
            verify(notificationMapper).insert(any(EqAlarmNotification.class));
        }
    }

    @Nested
    @DisplayName("离线告警")
    class OfflineAlarm {

        @Test
        @DisplayName("通过 deviceId 创建离线告警")
        void testCreateAlarmById_createsOfflineAlarm() {
            when(alarmMapper.findLatestActiveAlarm(2L, "OFFLINE")).thenReturn(null);
            when(alarmMapper.insert(any(EqDeviceAlarm.class))).thenAnswer(inv -> {
                EqDeviceAlarm a = inv.getArgument(0);
                a.setId(200L);
                return 1;
            });

            service.createAlarm(2L, "OFFLINE", "WARNING", "设备 D002 离线");

            verify(alarmMapper).insert(alarmCaptor.capture());
            assertThat(alarmCaptor.getValue().getAlarmType()).isEqualTo("OFFLINE");
            assertThat(alarmCaptor.getValue().getDeviceId()).isEqualTo(2L);

            verify(notificationMapper).insert(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue().getNotifyType()).isEqualTo("IN_APP");
        }
    }

    @Nested
    @DisplayName("语音拨号过滤（V2.0 下线）")
    class VoiceFilter {

        @Test
        @DisplayName("sendNotification：VOICE 类型被强制降级为 IN_APP")
        void testSendNotification_voiceFilteredToInApp() {
            EqAlarmNotification notification = new EqAlarmNotification();
            notification.setAlarmId(1L);
            notification.setNotifyType("VOICE");
            notification.setNotifyTarget("13800138000");
            notification.setNotifyContent("测试语音");

            service.sendNotification(notification);

            verify(notificationMapper).insert(notificationCaptor.capture());
            EqAlarmNotification saved = notificationCaptor.getValue();
            assertThat(saved.getNotifyType()).isEqualTo("IN_APP");
            assertThat(saved.getSendStatus()).isEqualTo("SENT");
        }

        @Test
        @DisplayName("sendNotification：正常类型不受影响")
        void testSendNotification_normalType_passesThrough() {
            EqAlarmNotification notification = new EqAlarmNotification();
            notification.setAlarmId(1L);
            notification.setNotifyType("EMAIL");
            notification.setNotifyContent("测试邮件");

            service.sendNotification(notification);

            verify(notificationMapper).insert(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue().getNotifyType()).isEqualTo("EMAIL");
        }
    }
}
