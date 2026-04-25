# kimi01 测试方案 - Sprint 2

> 版本: V1.0
> 日期: 2026-04-25
> 对应需求: EQ-007~EQ-014, MD-002, SYS-002, SYS-003

---

## 1. 测试策略总览

### 1.1 测试金字塔

```
        /\
       /  \   E2E 测试 (simulator.py 全流程)
      /----\
     /      \  集成测试 (Controller + Service + 内存DB)
    /--------\
   /          \ 单元测试 (Service Mock Mapper, Adapter Mock 网络)
  /------------\
```

### 1.2 测试分层策略

| 层级 | 目标 | 工具 | 覆盖率目标 |
|------|------|------|-----------|
| 单元测试 | Service/Adapter 逻辑分支 | JUnit 5 + Mockito | 80%+ |
| 集成测试 | Controller API + 参数校验 | MockMvc + @SpringBootTest | 核心路径 100% |
| E2E 测试 | 完整业务流程 | python simulator.py | 全流程通过 |

### 1.3 测试范围

| 模块 | 测试对象 | 配合开发 |
|------|---------|---------|
| dms-equipment | DeviceAdapter / AdapterRegistry / TcpAdapter / MqttAdapter | kimi02 + kimi03 |
| dms-equipment | DeviceShadow (reported/desired/version) | kimi03 |
| dms-equipment | HeartbeatScheduler (超时检测) | kimi03 |
| dms-masterdata | DecoctScheme CRUD | kimi04 |
| dms-system | SysConfig CRUD + 缓存 | kimi04 |
| dms-system | SysLog 查询 + 写入 | kimi04 |
| 全局 | 回归测试 (现有20个测试 + simulator.py) | kimi01 |

---

## 2. 测试用例清单

### 2.1 适配器测试 (配合 kimi02/kimi03)

#### TC-EQ-001: DeviceAdapter 接口契约验证
| 项 | 内容 |
|----|------|
| 描述 | 验证所有适配器实现均遵循 DeviceAdapter 接口契约 |
| 前置条件 | DeviceAdapter 接口已定义，至少2个实现类 |
| 步骤 | 1. 获取所有 DeviceAdapter 实现 Bean<br>2. 调用 connect / disconnect / sendCommand / parseStatus |
| 预期结果 | 所有实现类均不抛 UnsupportedOperationException，方法签名一致 |

#### TC-EQ-002: AdapterRegistry 按厂商路由
| 项 | 内容 |
|----|------|
| 描述 | 验证 AdapterRegistry 能根据 manufacturer + protocol 选择正确适配器 |
| 前置条件 | 注册中心已注入 4 个适配器（东华原TCP、三延TCP、仟方MQTT、厚达MQTT） |
| 步骤 | 1. 传入 manufacturer="东华原", protocol="TCP" → 获取适配器<br>2. 传入 manufacturer="仟方", protocol="MQTT" → 获取适配器<br>3. 传入未注册的厂商 → 获取结果 |
| 预期结果 | 1. 返回 DonghuaYuanAdapter<br>2. 返回 QianfangAdapter<br>3. 返回 null 或抛 IllegalArgumentException |

#### TC-EQ-003: TCP 适配器连接成功
| 项 | 内容 |
|----|------|
| 描述 | 验证 TCP 适配器能正确建立连接并发送指令 |
| 前置条件 | Mock SocketChannel / Netty Channel |
| 步骤 | 1. 调用 connect(deviceConnectionInfo)<br>2. 验证连接状态为 CONNECTED<br>3. 调用 sendCommand(deviceId, START)<br>4. 验证字节流正确写入 |
| 预期结果 | 连接建立成功，指令字节流与厂商协议规范一致 |

#### TC-EQ-004: TCP 适配器连接失败
| 项 | 内容 |
|----|------|
| 描述 | 验证 TCP 适配器在连接失败时抛出合理异常 |
| 前置条件 | Mock SocketChannel 抛出 IOException |
| 步骤 | 1. 调用 connect(deviceConnectionInfo) 模拟连接超时<br>2. 捕获异常 |
| 预期结果 | 抛出自定义 DeviceConnectException，内含设备编码和错误原因 |

#### TC-EQ-005: TCP 适配器状态解析
| 项 | 内容 |
|----|------|
| 描述 | 验证 TCP 二进制报文能正确解析为 DeviceStatus |
| 前置条件 | 准备东华原/三延的模拟二进制报文 |
| 步骤 | 1. 调用 parseStatus(东华原报文字节数组)<br>2. 调用 parseStatus(三延报文字节数组) |
| 预期结果 | 1. temperature=98.5℃, status=BUSY, faultCode=null<br>2. temperature=100.0℃, status=IDLE, faultCode=null |

#### TC-EQ-006: MQTT 适配器消息解析
| 项 | 内容 |
|----|------|
| 描述 | 验证 MQTT 适配器能正确解析不同厂商的 Topic + Payload |
| 前置条件 | Mock MqttClient，模拟仟方/厚达的消息到达 |
| 步骤 | 1. 模拟仟方 topic="qianfang/D001/status" payload={"temp":95,"state":1}<br>2. 模拟厚达 topic="houda/D002/data" payload={"temperature":97.5,"running":true} |
| 预期结果 | 1. 解析为 temperature=95, status=BUSY<br>2. 解析为 temperature=97.5, status=BUSY |

### 2.2 设备影子测试 (配合 kimi03)

#### TC-EQ-007: 影子 reported 状态更新
| 项 | 内容 |
|----|------|
| 描述 | 设备上报状态后，reported 态正确更新 |
| 前置条件 | DeviceShadowService 已初始化，设备 D001 无影子记录 |
| 步骤 | 1. 设备上报 status=BUSY, temp=98.5<br>2. 查询设备 D001 的影子 |
| 预期结果 | reported.status=BUSY, reported.temperature=98.5, version=1 |

#### TC-EQ-008: 影子 desired 状态设置
| 项 | 内容 |
|----|------|
| 描述 | 系统下发指令后，desired 态正确记录 |
| 前置条件 | 设备 D001 已有 reported 态 |
| 步骤 | 1. 系统调用 setDesired(D001, {command:"START", targetTemp:100})<br>2. 查询影子 |
| 预期结果 | desired.command=START, desired.targetTemp=100, version=2 |

#### TC-EQ-009: 影子版本号递增
| 项 | 内容 |
|----|------|
| 描述 | 每次更新 reported 或 desired，version 自动 +1 |
| 前置条件 | 设备 D001 影子 version=5 |
| 步骤 | 1. 更新 reported → 查 version<br>2. 更新 desired → 查 version |
| 预期结果 | 1. version=6<br>2. version=7 |

#### TC-EQ-010: 影子数据持久化
| 项 | 内容 |
|----|------|
| 描述 | 应用重启后影子数据不丢失 |
| 前置条件 | 数据库表 device_shadow 已存在 |
| 步骤 | 1. 写入影子数据<br>2. 模拟重启（新建 Service 实例从 DB 加载）<br>3. 查询影子 |
| 预期结果 | 数据与重启前一致 |

### 2.3 心跳超时测试 (配合 kimi03)

#### TC-EQ-011: 心跳正常刷新
| 项 | 内容 |
|----|------|
| 描述 | 设备正常发送心跳，lastHeartbeatTime 持续更新 |
| 前置条件 | HeartbeatScheduler 已启动，设备 D001 状态=IDLE |
| 步骤 | 1. 记录当前 lastHeartbeatTime=T1<br>2. 模拟心跳上报<br>3. 查询 lastHeartbeatTime |
| 预期结果 | lastHeartbeatTime > T1，设备状态仍为 IDLE |

#### TC-EQ-012: 心跳超时自动离线
| 项 | 内容 |
|----|------|
| 描述 | 设备超过阈值未发送心跳，自动标记为 OFFLINE |
| 前置条件 | 心跳阈值=60秒，设备 D001 状态=IDLE，lastHeartbeatTime=当前时间-120秒 |
| 步骤 | 1. 触发 HeartbeatScheduler 扫描任务<br>2. 查询设备 D001 状态 |
| 预期结果 | 设备状态变为 OFFLINE，记录告警日志 |

#### TC-EQ-013: 设备重新上线恢复
| 项 | 内容 |
|----|------|
| 描述 | OFFLINE 设备重新发送心跳后恢复为 IDLE |
| 前置条件 | 设备 D001 状态=OFFLINE |
| 步骤 | 1. 模拟心跳上报<br>2. 触发扫描任务<br>3. 查询设备状态 |
| 预期结果 | 设备状态恢复为 IDLE |

### 2.4 CRUD 接口测试 (配合 kimi04)

#### TC-MD-001: 煎药方案创建
| 项 | 内容 |
|----|------|
| 描述 | 验证 DecoctScheme CRUD 之创建 |
| 前置条件 | 数据库 md_decoct_scheme 表已就绪 |
| 步骤 | POST /api/v1/md/schemes {name:"常压汤药", schemeType:0, decoctTimes:0, pressure:1, upperWater:0, heatingTime:30} |
| 预期结果 | code=200, data.id 不为 null, name="常压汤药" |

#### TC-MD-002: 煎药方案分页查询
| 项 | 内容 |
|----|------|
| 描述 | 验证分页 + 关键字搜索 |
| 前置条件 | 已创建3条方案记录 |
| 步骤 | GET /api/v1/md/schemes?page=1&size=2&keyword="常压" |
| 预期结果 | records.size=2, total>=3, 所有记录 name 包含"常压" |

#### TC-SYS-001: 系统配置创建与缓存
| 项 | 内容 |
|----|------|
| 描述 | 验证配置创建后写入缓存 |
| 前置条件 | SysConfigService 已初始化，带内存缓存 |
| 步骤 | 1. POST /api/v1/sys/configs {configKey:"soak.timeout", configValue:"30", description:"泡药超时分钟"}<br>2. GET /api/v1/sys/configs/key/soak.timeout<br>3. 直接查数据库验证 |
| 预期结果 | 1. code=200<br>2. configValue="30"<br>3. 数据库记录一致，且缓存命中（可通过日志或第二次查询延迟验证） |

#### TC-SYS-002: 系统配置更新后缓存失效
| 项 | 内容 |
|----|------|
| 描述 | 验证配置更新后缓存自动刷新 |
| 前置条件 | 配置 soak.timeout 已在缓存中，value="30" |
| 步骤 | 1. PUT /api/v1/sys/configs/{id} {configValue:"45"}<br>2. GET /api/v1/sys/configs/key/soak.timeout |
| 预期结果 | 第二次查询返回 "45"，非旧值 "30" |

#### TC-SYS-003: 操作日志写入
| 项 | 内容 |
|----|------|
| 描述 | 验证 SysLogService.log() 正确写入日志 |
| 前置条件 | sys_log 表已就绪 |
| 步骤 | 1. 调用 sysLogService.log("OP001", "CREATE_PRESCRIPTION", "production", "创建处方ID=100", "192.168.1.1")<br>2. 查询 sys_log 表 |
| 预期结果 | 表中新增一条记录，各字段与入参一致 |

#### TC-SYS-004: 操作日志按模块过滤查询
| 项 | 内容 |
|----|------|
| 描述 | 验证日志查询支持按模块/用户/时间范围过滤 |
| 前置条件 | 已写入多条不同模块的日志 |
| 步骤 | GET /api/v1/sys/logs?module=production&userId=OP001&startTime=2026-04-01&endTime=2026-04-30 |
| 预期结果 | records 中所有记录 module="production" 且 userId="OP001"，且在时间范围内 |

### 2.5 回归测试清单

| 测试项 | 测试类/方法 | 类型 |
|--------|------------|------|
| 全流程 V3 | FullWorkflowTest#fullWorkflow_v3_fromPrescriptionToHandover | 集成 |
| 全流程 V2 | FullWorkflowTest#fullWorkflow_fromPrescriptionToCompleted | 集成 |
| 质检返工 | FullWorkflowTest#qualityInspect_rework_shouldResetToDecoct | 集成 |
| 质检报废 | FullWorkflowTest#qualityInspect_scrap_shouldMarkException | 集成 |
| 部分交接 | FullWorkflowTest#handover_partial_shouldMarkPartialCompleted | 集成 |
| 强制操作 | FullWorkflowTest#forceStatus_shouldWork | 集成 |
| 非法流转 | FullWorkflowTest#illegalStateTransition_shouldFail | 集成 |
| 设备绑定 | TaskControllerTest#bindDevice_success | 集成 |
| 设备冲突 | TaskControllerTest#bindDevice_deviceAlreadyBound | 集成 |
| 并发绑定 | ConcurrentBindTest | 集成 |
| MQTT 断连 | MqttDisconnectTest | 集成 |
| 处方创建 | PrescriptionControllerTest | 集成 |
| 应用启动 | DecoctionApplicationTests | 集成 |
| Simulator V3 | python3 simulator.py | E2E |

---

## 3. 测试数据准备方案

### 3.1 数据库初始化

```sql
-- 设备数据（适配器测试用）
INSERT INTO device (device_code, name, device_type, status, manufacturer, protocol, ip_address, port)
VALUES 
  ('DHY-001', '东华原煎药机1号', 1, 'IDLE', '东华原', 'TCP', '192.168.1.101', 502),
  ('SY-001', '三延煎药机1号', 1, 'IDLE', '三延', 'TCP', '192.168.1.102', 8080),
  ('QF-001', '仟方煎药机1号', 1, 'IDLE', '仟方', 'MQTT', NULL, NULL),
  ('HD-001', '厚达煎药机1号', 1, 'IDLE', '厚达', 'MQTT', NULL, NULL);

-- 煎药方案数据
INSERT INTO md_decoct_scheme (name, scheme_type, decoct_times, pressure, upper_water, heating_time, description)
VALUES ('测试方案A', 0, 0, 1, 0, 30, '测试用');

-- 系统配置数据
INSERT INTO sys_config (config_key, config_value, description)
VALUES ('soak.timeout', '30', '泡药超时分钟');
```

### 3.2 模拟报文数据

| 厂商 | 类型 | 模拟数据 |
|------|------|---------|
| 东华原 | 状态报文 | `0x01 0x01 0x00 0x62 0x01 0x00` (BUSY, 98.5℃) |
| 三延 | 状态报文 | `0xAA 0x55 0x01 0x00 0x64 0x00` (IDLE, 100℃) |
| 仟方 | MQTT Payload | `{"deviceId":"QF-001","temp":95,"state":1}` |
| 厚达 | MQTT Payload | `{"deviceId":"HD-001","temperature":97.5,"running":true}` |

---

## 4. Mock 策略

### 4.1 TCP 连接 Mock

```java
// 方案A: 使用 Mockito Mock Socket/Channel
@Mock
private SocketChannel socketChannel;

@Test
void tcpAdapter_connectSuccess() {
    when(socketChannel.connect(any())).thenReturn(true);
    TcpAdapter adapter = new TcpAdapter(socketChannel);
    assertDoesNotThrow(() -> adapter.connect(info));
}

// 方案B: 提取 TcpConnectionFactory 接口（推荐，利于测试）
public interface TcpConnectionFactory {
    SocketChannel createChannel(String host, int port);
}

@Test
void tcpAdapter_connectSuccess_withFactory() {
    TcpConnectionFactory factory = (h, p) -> mockSocketChannel;
    TcpAdapter adapter = new TcpAdapter(factory);
    // ...
}
```

### 4.2 MQTT 连接 Mock

```java
// MqttConfig 中直接 new MqttClient()，建议提取 MqttClientFactory
@Mock
private MqttClient mqttClient;

@Test
void mqttAdapter_parseStatus() {
    MqttAdapter adapter = new QianfangAdapter();
    // 直接调用 parseStatus，无需真实 MQTT 连接
    DeviceStatus status = adapter.parseStatus(qianfangPayload.getBytes());
    assertEquals(BUSY, status.getStatus());
}
```

### 4.3 数据库 Mapper Mock

```java
@ExtendWith(MockitoExtension.class)
class DecoctSchemeServiceImplTest {
    @Mock
    private DecoctSchemeMapper mapper;
    
    @InjectMocks
    private DecoctSchemeServiceImpl service;
    
    @Test
    void create() {
        when(mapper.insert(any())).thenAnswer(inv -> {
            DecoctScheme s = inv.getArgument(0);
            s.setId(1L);
            return 1;
        });
        DecoctScheme result = service.create(request);
        assertEquals(1L, result.getId());
    }
}
```

### 4.4 调度器时间控制

```java
// 心跳测试使用 @Scheduled，测试时可通过以下方式控制时间：
// 方案A: 手动触发扫描方法（包可见或测试专用入口）
heartbeatScheduler.checkTimeout();

// 方案B: 使用 Awaitility 等待调度执行
await().atMost(5, SECONDS).until(() -> 
    deviceService.getById("D001").getStatus().equals("OFFLINE")
);
```

---

## 5. 覆盖率目标

| 模块 | 目标覆盖率 | 重点覆盖类 |
|------|-----------|-----------|
| dms-equipment adapter | > 85% | *Adapter, AdapterRegistry |
| dms-equipment shadow | > 85% | DeviceShadowService |
| dms-equipment heartbeat | > 80% | HeartbeatScheduler |
| dms-masterdata | > 80% | DecoctSchemeServiceImpl, DecoctSchemeController |
| dms-system | > 80% | SysConfigServiceImpl, SysLogServiceImpl |
| 全局回归 | 100% | 现有20个集成测试全部通过 |

---

## 6. 测试执行计划

| 阶段 | 时间 | 内容 |
|------|------|------|
| 开发并行期 | Sprint 3 (5天) | 与开发同步编写单元测试，Mock 先行 |
| 集成测试期 | Sprint 4 前2天 | 执行 Controller 集成测试，验证 API 契约 |
| 回归测试期 | Sprint 4 第3天 | 执行全部20个回归测试 + simulator.py |
| 覆盖率检查 | Sprint 4 最后一天 | `mvn jacoco:report`，确认 > 80% |

---

## 7. 风险与应对

| 风险 | 影响 | 应对策略 |
|------|------|---------|
| MQTT/TCP 真实连接难以测试 | 中 | 提取 Factory 接口，全部 Mock |
| @Scheduled 心跳测试不稳定 | 中 | 提供手动触发入口，避免依赖定时 |
| 设备影子存储方案未定 | 高 | 方案中同时覆盖内存+DB两种假设 |
| sys_config 缓存策略未定 | 中 | 测试用例同时验证 DB 一致性和缓存行为 |

---

*本方案待架构师审核通过后执行。*
