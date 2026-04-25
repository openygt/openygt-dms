package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.common.dto.EqDeviceDTO;
import cn.org.openygt.common.dto.TemperatureThresholdDTO;
import cn.org.openygt.common.service.SysConfigService;
import cn.org.openygt.equipment.entity.EqDevice;
import cn.org.openygt.equipment.entity.EqDeviceAlarm;
import cn.org.openygt.equipment.mapper.EqDeviceAlarmMapper;
import cn.org.openygt.equipment.mapper.EqDeviceMapper;
import cn.org.openygt.masterdata.entity.DecoctScheme;
import cn.org.openygt.masterdata.service.DecoctSchemeService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EquipmentServiceImpl 单元测试。
 *
 * <p>覆盖场景：
 * <ul>
 *   <li>温度阈值三层继承（SYSTEM / DEVICE / SCHEME）</li>
 *   <li>reserveDevice 悲观锁 + 状态校验</li>
 *   <li>releaseDevice 悲观锁</li>
 *   <li>toDTO 字段映射</li>
 *   <li>checkTemperatureAlarm 告警逻辑</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class EquipmentServiceImplTest {

    @Mock
    private EqDeviceMapper deviceMapper;

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private DecoctSchemeService decoctSchemeService;

    @Mock
    private EqDeviceAlarmMapper alarmMapper;

    @Captor
    private ArgumentCaptor<EqDeviceAlarm> alarmCaptor;

    private EquipmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EquipmentServiceImpl(deviceMapper, sysConfigService, decoctSchemeService, alarmMapper);
    }

    @Nested
    @DisplayName("温度阈值三层继承")
    class ThresholdInheritance {

        @Test
        @DisplayName("设备级阈值优先于系统默认")
        void testDeviceLevelWins() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");
            device.setAlarmMaxTemp(new BigDecimal("105.0"));
            device.setAlarmMinTemp(new BigDecimal("85.0"));

            when(deviceMapper.selectById(1L)).thenReturn(device);

            TemperatureThresholdDTO result = service.getEffectiveThreshold(1L);

            assertThat(result.getHighTemp()).isEqualByComparingTo(new BigDecimal("105.0"));
            assertThat(result.getLowTemp()).isEqualByComparingTo(new BigDecimal("85.0"));
            assertThat(result.getSource()).isEqualTo("DEVICE");
        }

        @Test
        @DisplayName("设备级未配置 → 回退到系统默认")
        void testFallbackToSystem() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");
            device.setAlarmMaxTemp(null);
            device.setAlarmMinTemp(null);

            when(deviceMapper.selectById(1L)).thenReturn(device);
            when(sysConfigService.getStringValue(anyString(), anyString())).thenAnswer(
                    inv -> inv.getArgument(1)); // 返回 defaultValue

            TemperatureThresholdDTO result = service.getEffectiveThreshold(1L);

            assertThat(result.getHighTemp()).isEqualByComparingTo(new BigDecimal("110.0"));
            assertThat(result.getLowTemp()).isEqualByComparingTo(new BigDecimal("80.0"));
            assertThat(result.getSource()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("方案级覆盖设备级和系统默认")
        void testSchemeLevelOverrides() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");
            device.setAlarmMaxTemp(new BigDecimal("105.0")); // 设备级有值但被方案覆盖
            device.setAlarmMinTemp(new BigDecimal("85.0"));
            device.setCurrentSchemeId(100L);

            DecoctScheme scheme = new DecoctScheme();
            scheme.setId(100L);
            scheme.setAlarmHighTemp(new BigDecimal("100.0"));
            scheme.setAlarmLowTemp(new BigDecimal("90.0"));

            when(deviceMapper.selectById(1L)).thenReturn(device);
            when(decoctSchemeService.getById(100L)).thenReturn(scheme);

            TemperatureThresholdDTO result = service.getEffectiveThreshold(1L);

            assertThat(result.getHighTemp()).isEqualByComparingTo(new BigDecimal("100.0"));
            assertThat(result.getLowTemp()).isEqualByComparingTo(new BigDecimal("90.0"));
            assertThat(result.getSource()).isEqualTo("SCHEME");
        }

        @Test
        @DisplayName("方案有 schemeId 但 scheme 为空 → 回退到设备级")
        void testSchemeNotFound_fallsBackToDevice() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");
            device.setAlarmMaxTemp(new BigDecimal("105.0"));
            device.setAlarmMinTemp(new BigDecimal("85.0"));
            device.setCurrentSchemeId(999L);

            when(deviceMapper.selectById(1L)).thenReturn(device);
            when(decoctSchemeService.getById(999L)).thenReturn(null);

            TemperatureThresholdDTO result = service.getEffectiveThreshold(1L);

            assertThat(result.getHighTemp()).isEqualByComparingTo(new BigDecimal("105.0"));
            assertThat(result.getSource()).isEqualTo("DEVICE");
        }

        @Test
        @DisplayName("配置键为 temp.alarm.high / temp.alarm.low（非旧格式）")
        void testConfigKeysUseNewFormat() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");
            device.setAlarmMaxTemp(null);
            device.setAlarmMinTemp(null);

            when(deviceMapper.selectById(1L)).thenReturn(device);
            when(sysConfigService.getStringValue("temp.alarm.high", "110.0")).thenReturn("120.0");
            when(sysConfigService.getStringValue("temp.alarm.low", "80.0")).thenReturn("70.0");

            TemperatureThresholdDTO result = service.getEffectiveThreshold(1L);

            assertThat(result.getHighTemp()).isEqualByComparingTo(new BigDecimal("120.0"));
            assertThat(result.getLowTemp()).isEqualByComparingTo(new BigDecimal("70.0"));
            assertThat(result.getSource()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("方案查询异常 → 回退到设备级")
        void testSchemeException_fallsBackToDevice() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");
            device.setAlarmMaxTemp(new BigDecimal("105.0"));
            device.setAlarmMinTemp(new BigDecimal("85.0"));
            device.setCurrentSchemeId(100L);

            when(deviceMapper.selectById(1L)).thenReturn(device);
            when(decoctSchemeService.getById(100L)).thenThrow(new RuntimeException("DB异常"));

            TemperatureThresholdDTO result = service.getEffectiveThreshold(1L);

            assertThat(result.getHighTemp()).isEqualByComparingTo(new BigDecimal("105.0"));
            assertThat(result.getSource()).isEqualTo("DEVICE");
        }

        @Test
        @DisplayName("系统配置为空字符串 → 使用默认值")
        void testEmptyConfig_fallsBackToDefault() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");
            device.setAlarmMaxTemp(null);
            device.setAlarmMinTemp(null);

            when(deviceMapper.selectById(1L)).thenReturn(device);
            when(sysConfigService.getStringValue("temp.alarm.high", "110.0")).thenReturn("");
            when(sysConfigService.getStringValue("temp.alarm.low", "80.0")).thenReturn("");

            TemperatureThresholdDTO result = service.getEffectiveThreshold(1L);

            assertThat(result.getHighTemp()).isEqualByComparingTo(new BigDecimal("110.0"));
            assertThat(result.getLowTemp()).isEqualByComparingTo(new BigDecimal("80.0"));
            assertThat(result.getSource()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("系统配置为非法数字 → 使用默认值")
        void testInvalidConfig_fallsBackToDefault() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");
            device.setAlarmMaxTemp(null);
            device.setAlarmMinTemp(null);

            when(deviceMapper.selectById(1L)).thenReturn(device);
            when(sysConfigService.getStringValue("temp.alarm.high", "110.0")).thenReturn("not-a-number");
            when(sysConfigService.getStringValue("temp.alarm.low", "80.0")).thenReturn("80.0");

            TemperatureThresholdDTO result = service.getEffectiveThreshold(1L);

            assertThat(result.getHighTemp()).isEqualByComparingTo(new BigDecimal("110.0"));
        }

        @Test
        @DisplayName("设备不存在 → getEffectiveThreshold 抛出异常")
        void testDeviceNotFound_getEffectiveThreshold_throws() {
            when(deviceMapper.selectById(99L)).thenReturn(null);

            assertThatThrownBy(() -> service.getEffectiveThreshold(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不存在");
        }
    }

    @Nested
    @DisplayName("设备预留/释放")
    class ReserveRelease {

        @Test
        @DisplayName("reserveDevice：IDLE 设备预留成功")
        void testReserveDevice_idleDevice_succeeds() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setStatus("IDLE");

            when(deviceMapper.selectForUpdate(1L)).thenReturn(device);

            service.reserveDevice(100L, 1L);

            verify(deviceMapper).update(isNull(), any(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class));
        }

        @Test
        @DisplayName("reserveDevice：RESERVED 设备允许再次预留（返工场景）")
        void testReserveDevice_reservedDevice_allowsForRework() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setStatus("RESERVED");

            when(deviceMapper.selectForUpdate(1L)).thenReturn(device);

            service.reserveDevice(100L, 1L);

            verify(deviceMapper).update(isNull(), any(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class));
        }

        @Test
        @DisplayName("reserveDevice：RUNNING 设备预留失败")
        void testReserveDevice_runningDevice_throws() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setStatus("RUNNING");

            when(deviceMapper.selectForUpdate(1L)).thenReturn(device);

            assertThatThrownBy(() -> service.reserveDevice(100L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("RUNNING");

            verify(deviceMapper, never()).update(isNull(), any(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class));
        }

        @Test
        @DisplayName("reserveDevice：使用 selectForUpdate 悲观锁")
        void testReserveDevice_usesSelectForUpdate() {
            when(deviceMapper.selectForUpdate(1L)).thenReturn(null);

            assertThatThrownBy(() -> service.reserveDevice(100L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不存在");

            verify(deviceMapper, never()).selectById(1L);
        }

        @Test
        @DisplayName("releaseDevice：使用 selectForUpdate 悲观锁")
        void testReleaseDevice_usesSelectForUpdate() {
            EqDevice device = new EqDevice();
            device.setId(1L);

            when(deviceMapper.selectForUpdate(1L)).thenReturn(device);

            service.releaseDevice(1L);

            verify(deviceMapper).selectForUpdate(1L);
            verify(deviceMapper).update(isNull(), any(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class));
        }

        @Test
        @DisplayName("releaseDevice：设备不存在时静默忽略")
        void testReleaseDevice_deviceNotFound_ignored() {
            when(deviceMapper.selectForUpdate(1L)).thenReturn(null);

            service.releaseDevice(1L);

            verify(deviceMapper, never()).update(isNull(), any(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class));
        }
    }

    @Nested
    @DisplayName("DTO 字段映射")
    class DtoMapping {

        @Test
        @DisplayName("toDTO：alarmMaxTemp → alarmHighTemp，alarmMinTemp → alarmLowTemp")
        void testToDTO_mapsAlarmFields() {
            EqDevice entity = new EqDevice();
            entity.setId(1L);
            entity.setDeviceCode("D001");
            entity.setName("Test Device");
            entity.setDeviceType(1);
            entity.setStatus("IDLE");
            entity.setCurrentTemp(new BigDecimal("98.5"));
            entity.setAlarmMaxTemp(new BigDecimal("105.0"));
            entity.setAlarmMinTemp(new BigDecimal("85.0"));

            EqDeviceDTO dto = service.getDeviceById(1L);

            // 通过间接方式获取 — 直接测到DTO映射
            // 这里用 mock 验证：selectById 后调用 toDTO
            when(deviceMapper.selectById(1L)).thenReturn(entity);

            dto = service.getDeviceById(1L);

            assertThat(dto).isNotNull();
            assertThat(dto.getDeviceCode()).isEqualTo("D001");
            assertThat(dto.getAlarmHighTemp()).isEqualByComparingTo(new BigDecimal("105.0"));
            assertThat(dto.getAlarmLowTemp()).isEqualByComparingTo(new BigDecimal("85.0"));
        }
    }

    @Nested
    @DisplayName("温度告警检测")
    class TemperatureAlarm {

        private EqDevice device;

        @BeforeEach
        void setUp() {
            device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");
            device.setAlarmMaxTemp(new BigDecimal("100.0"));
            device.setAlarmMinTemp(new BigDecimal("80.0"));
        }

        @Test
        @DisplayName("温度未超阈值 → 不创建告警")
        void testWithinThreshold_noAlarm() {
            when(deviceMapper.selectById(1L)).thenReturn(device);

            service.checkTemperatureAlarm(1L, new BigDecimal("90.0"));

            verify(alarmMapper, never()).insert(any(EqDeviceAlarm.class));
        }

        @Test
        @DisplayName("超高温 → 创建 HIGH_TEMP 告警")
        void testHighTemp_createsAlarm() {
            when(deviceMapper.selectById(1L)).thenReturn(device);
            when(alarmMapper.findLatestActiveAlarm(1L, "HIGH_TEMP")).thenReturn(null);

            service.checkTemperatureAlarm(1L, new BigDecimal("105.0"));

            verify(alarmMapper).insert(alarmCaptor.capture());
            EqDeviceAlarm alarm = alarmCaptor.getValue();
            assertThat(alarm.getAlarmType()).isEqualTo("HIGH_TEMP");
            assertThat(alarm.getAlarmLevel()).isEqualTo("CRITICAL");
            assertThat(alarm.getIsResolved()).isEqualTo(0);
        }

        @Test
        @DisplayName("超低温 → 创建 LOW_TEMP 告警")
        void testLowTemp_createsAlarm() {
            when(deviceMapper.selectById(1L)).thenReturn(device);
            when(alarmMapper.findLatestActiveAlarm(1L, "HIGH_TEMP")).thenReturn(null);

            service.checkTemperatureAlarm(1L, new BigDecimal("75.0"));

            verify(alarmMapper).insert(alarmCaptor.capture());
            EqDeviceAlarm alarm = alarmCaptor.getValue();
            assertThat(alarm.getAlarmType()).isEqualTo("LOW_TEMP");
            assertThat(alarm.getAlarmLevel()).isEqualTo("WARNING");
        }

        @Test
        @DisplayName("超高温但冷却期内（<5min）→ 不重复创建告警")
        void testHighTemp_inCooldown_skipsAlarm() {
            EqDeviceAlarm recentAlarm = new EqDeviceAlarm();
            recentAlarm.setCreatedAt(LocalDateTime.now().minusMinutes(2)); // 2分钟前

            when(deviceMapper.selectById(1L)).thenReturn(device);
            when(alarmMapper.findLatestActiveAlarm(1L, "HIGH_TEMP")).thenReturn(recentAlarm);

            service.checkTemperatureAlarm(1L, new BigDecimal("105.0"));

            verify(alarmMapper, never()).insert(any(EqDeviceAlarm.class));
        }

        @Test
        @DisplayName("超低温但冷却期内（<5min）→ 不重复创建告警")
        void testLowTemp_inCooldown_skipsAlarm() {
            EqDeviceAlarm recentAlarm = new EqDeviceAlarm();
            recentAlarm.setCreatedAt(LocalDateTime.now().minusMinutes(3)); // 3分钟前

            when(deviceMapper.selectById(1L)).thenReturn(device);
            when(alarmMapper.findLatestActiveAlarm(1L, "HIGH_TEMP")).thenReturn(null);
            when(alarmMapper.findLatestActiveAlarm(1L, "LOW_TEMP")).thenReturn(recentAlarm);

            service.checkTemperatureAlarm(1L, new BigDecimal("75.0"));

            verify(alarmMapper, never()).insert(any(EqDeviceAlarm.class));
        }

        @Test
        @DisplayName("温度恢复正常 → 解析活跃告警")
        void testTempNormal_resolvesAlarms() {
            when(deviceMapper.selectById(1L)).thenReturn(device);
            when(alarmMapper.findLatestActiveAlarm(1L, "HIGH_TEMP")).thenReturn(null);
            when(alarmMapper.findActiveByDeviceIdAndType(1L, "HIGH_TEMP"))
                    .thenReturn(Collections.emptyList());
            when(alarmMapper.findActiveByDeviceIdAndType(1L, "LOW_TEMP"))
                    .thenReturn(Collections.emptyList());

            service.checkTemperatureAlarm(1L, new BigDecimal("90.0"));

            verify(alarmMapper).findActiveByDeviceIdAndType(1L, "HIGH_TEMP");
            verify(alarmMapper).findActiveByDeviceIdAndType(1L, "LOW_TEMP");
        }

        @Test
        @DisplayName("设备不存在 → 静默忽略")
        void testDeviceNotFound_ignored() {
            when(deviceMapper.selectById(99L)).thenReturn(null);

            service.checkTemperatureAlarm(99L, new BigDecimal("90.0"));

            verify(alarmMapper, never()).insert(any(EqDeviceAlarm.class));
            verify(alarmMapper, never()).findLatestActiveAlarm(anyLong(), anyString());
        }

        @Test
        @DisplayName("温度恢复正常 → 解析活跃告警（lambda 路径）")
        void testTempNormal_resolvesActiveAlarms_updatesTimestamp() {
            EqDeviceAlarm activeAlarm = new EqDeviceAlarm();
            activeAlarm.setId(1L);
            activeAlarm.setDeviceId(1L);
            activeAlarm.setIsResolved(0);

            when(deviceMapper.selectById(1L)).thenReturn(device);
            when(alarmMapper.findActiveByDeviceIdAndType(1L, "HIGH_TEMP"))
                    .thenReturn(Collections.singletonList(activeAlarm));
            when(alarmMapper.findActiveByDeviceIdAndType(1L, "LOW_TEMP"))
                    .thenReturn(Collections.emptyList());

            service.checkTemperatureAlarm(1L, new BigDecimal("90.0"));

            verify(alarmMapper).updateById(activeAlarm);
            assertThat(activeAlarm.getIsResolved()).isEqualTo(1);
            assertThat(activeAlarm.getResolvedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("设备查询")
    class DeviceQuery {

        @Test
        @DisplayName("getDeviceByCode：设备存在返回 DTO")
        void testGetDeviceByCode_found() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");
            device.setName("Test");

            when(deviceMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class)))
                    .thenReturn(device);

            EqDeviceDTO result = service.getDeviceByCode("D001");

            assertThat(result).isNotNull();
            assertThat(result.getDeviceCode()).isEqualTo("D001");
        }

        @Test
        @DisplayName("getDeviceByCode：设备不存在返回 null")
        void testGetDeviceByCode_notFound() {
            when(deviceMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class)))
                    .thenReturn(null);

            assertThat(service.getDeviceByCode("D001")).isNull();
        }

        @Test
        @DisplayName("getDeviceId：设备存在返回 id")
        void testGetDeviceId_found() {
            EqDevice device = new EqDevice();
            device.setId(42L);
            device.setDeviceCode("D001");

            when(deviceMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class)))
                    .thenReturn(device);

            assertThat(service.getDeviceId("D001")).isEqualTo(42L);
        }

        @Test
        @DisplayName("getDeviceId：设备不存在返回 null")
        void testGetDeviceId_notFound() {
            when(deviceMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class)))
                    .thenReturn(null);

            assertThat(service.getDeviceId("D001")).isNull();
        }

        @Test
        @DisplayName("getDeviceStatus：设备存在返回状态")
        void testGetDeviceStatus_found() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setStatus("IDLE");

            when(deviceMapper.selectById(1L)).thenReturn(device);

            assertThat(service.getDeviceStatus(1L)).isEqualTo("IDLE");
        }

        @Test
        @DisplayName("getDeviceStatus：设备不存在返回 null")
        void testGetDeviceStatus_notFound() {
            when(deviceMapper.selectById(1L)).thenReturn(null);

            assertThat(service.getDeviceStatus(1L)).isNull();
        }

        @Test
        @DisplayName("getAutoLevel：设备存在返回等级")
        void testGetAutoLevel_found() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setAutoLevel("A");

            when(deviceMapper.selectById(1L)).thenReturn(device);

            assertThat(service.getAutoLevel(1L)).isEqualTo("A");
        }

        @Test
        @DisplayName("getDeviceCode：设备存在返回编码")
        void testGetDeviceCode_found() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");

            when(deviceMapper.selectById(1L)).thenReturn(device);

            assertThat(service.getDeviceCode(1L)).isEqualTo("D001");
        }
    }

    @Nested
    @DisplayName("设备状态管理")
    class StatusManagement {

        @Test
        @DisplayName("getOrCreateDevice：设备已存在直接返回")
        void testGetOrCreateDevice_existing() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setDeviceCode("D001");
            device.setName("D001");
            device.setStatus("IDLE");

            when(deviceMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class)))
                    .thenReturn(device);

            EqDeviceDTO result = service.getOrCreateDevice("D001", 1);

            assertThat(result.getDeviceCode()).isEqualTo("D001");
            verify(deviceMapper, never()).insert(any(EqDevice.class));
        }

        @Test
        @DisplayName("getOrCreateDevice：设备不存在则创建")
        void testGetOrCreateDevice_creates() {
            when(deviceMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class)))
                    .thenReturn(null);
            when(deviceMapper.insert(any(EqDevice.class))).thenReturn(1);

            EqDeviceDTO result = service.getOrCreateDevice("D002", 2);

            assertThat(result.getDeviceCode()).isEqualTo("D002");
            verify(deviceMapper).insert(any(EqDevice.class));
        }

        @Test
        @DisplayName("updateDeviceStatus：更新状态和 updated_at")
        void testUpdateDeviceStatus() {
            service.updateDeviceStatus(1L, "RUNNING");

            verify(deviceMapper).update(isNull(), any(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class));
        }

        @Test
        @DisplayName("updateTemperature：更新温度并触发告警检查")
        void testUpdateTemperature() {
            EqDevice device = new EqDevice();
            device.setId(1L);
            device.setAlarmMaxTemp(new BigDecimal("100.0"));
            device.setAlarmMinTemp(new BigDecimal("80.0"));

            when(deviceMapper.selectById(1L)).thenReturn(device);

            service.updateTemperature(1L, new BigDecimal("95.0"));

            verify(deviceMapper).update(isNull(), any(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class));
            verify(alarmMapper, never()).insert(any(EqDeviceAlarm.class));
        }
    }

    @Nested
    @DisplayName("设备故障管理")
    class FaultManagement {

        @Test
        @DisplayName("reportFault：设备状态变更为 FAULT")
        void testReportFault() {
            service.reportFault(1L, "E001", "测试故障");

            verify(deviceMapper).update(isNull(), any(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class));
        }

        @Test
        @DisplayName("clearFault：设备故障清除状态变更为 IDLE")
        void testClearFault() {
            service.clearFault(1L);

            verify(deviceMapper).update(isNull(), any(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class));
        }
    }

    @Nested
    @DisplayName("桩方法 / 待实现接口")
    class StubMethods {

        @Test
        @DisplayName("getOnlineDeviceCount 返回 0")
        void testGetOnlineDeviceCount() {
            assertThat(service.getOnlineDeviceCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("getFaultStats 返回空列表")
        void testGetFaultStats() {
            assertThat(service.getFaultStats(null, null)).isEmpty();
        }

        @Test
        @DisplayName("getDeviceUtilization 返回空 DTO")
        void testGetDeviceUtilization() {
            assertThat(service.getDeviceUtilization(1L, null, null)).isNotNull();
        }

        @Test
        @DisplayName("getDeviceCode：设备不存在返回 null")
        void testGetDeviceCode_notFound() {
            when(deviceMapper.selectById(99L)).thenReturn(null);

            assertThat(service.getDeviceCode(99L)).isNull();
        }
    }
}
