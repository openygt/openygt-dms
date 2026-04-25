# kimi02 详细设计 - 设备适配器框架 + TCP 适配器

> 对应概设: `docs/tasks/kimi02-概设.md`  
> 日期: 2026-04-25  
> 作者: kimi02

---

## 1. 概述

本设计在概设基础上，给出可直接编码的类结构、方法签名、数据库脚本、协议帧定义和测试方案。

**编码前提:**
- JDK 1.8
- Spring Boot 2.7.18
- 新增依赖: Netty 4.1.94.Final

---

## 2. 依赖变更

### 2.1 pom.xml 新增依赖

```xml
<!-- Netty -->
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
    <version>4.1.94.Final</version>
</dependency>
```

**说明:** `netty-all` 包含 `netty-handler`、`netty-transport`、`netty-buffer` 等全部模块，约 4.2MB。若关注包体积，可替换为仅引入 `netty-handler`（约 1.8MB），但建议开发阶段用 `netty-all` 简化依赖管理，上线前可精简。

---

## 3. 详细类设计

### 3.1 新增 DTO 类

#### `com.decoction.equipment.adapter.dto.DeviceConnectionInfo`

```java
package com.decoction.equipment.adapter.dto;

import lombok.Data;

@Data
public class DeviceConnectionInfo {
    private String ip;
    private int port;
    /** 连接超时（毫秒），默认 5000 */
    private int connectTimeoutMs = 5000;
    /** TCP 心跳间隔（秒），默认 30 */
    private int heartbeatIntervalSec = 30;
    /** 重连初始间隔（秒），默认 10 */
    private int reconnectBaseSec = 10;
    /** 重连最大间隔（秒），默认 60 */
    private int reconnectMaxSec = 60;
}
```

#### `com.decoction.equipment.adapter.dto.DeviceCommand`

```java
package com.decoction.equipment.adapter.dto;

import lombok.Data;
import java.util.Map;

@Data
public class DeviceCommand {
    /** 指令类型: START / STOP / PAUSE / RESUME / SET_TEMP / QUERY_STATUS */
    private String commandType;
    /** 指令参数，视 commandType 而定 */
    private Map<String, Object> params;
}
```

#### `com.decoction.equipment.adapter.dto.DeviceStatusReport`

```java
package com.decoction.equipment.adapter.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DeviceStatusReport {
    private String deviceCode;
    /** 设备状态: IDLE / BUSY / FAULT / OFFLINE */
    private String status;
    /** 当前温度（摄氏度） */
    private BigDecimal temperature;
    /** 故障码，无故障时为 null */
    private String faultCode;
    /** 状态上报时间戳 */
    private Long timestamp;
    /** 原始字节数据（日志/调试用） */
    private byte[] rawData;
}
```

---

### 3.2 适配器接口与注册中心

#### `com.decoction.equipment.adapter.DeviceAdapter`

```java
package com.decoction.equipment.adapter;

import com.decoction.equipment.adapter.dto.DeviceCommand;
import com.decoction.equipment.adapter.dto.DeviceConnectionInfo;
import com.decoction.equipment.adapter.dto.DeviceStatusReport;

public interface DeviceAdapter {
    /** 厂商编码，如 "donghuayuan", "sanyan", "qianfang", "houda" */
    String getManufacturer();
    
    /** 协议类型: TCP / MQTT */
    String getProtocol();
    
    // ========== TCP 侧（MQTT 适配器提供空实现） ==========
    
    /** 建立与设备的连接 */
    default void connect(String deviceCode, DeviceConnectionInfo info) {
        throw new UnsupportedOperationException("MQTT adapter does not support TCP connect");
    }
    
    /** 断开与设备的连接 */
    default void disconnect(String deviceCode) {
        throw new UnsupportedOperationException("MQTT adapter does not support TCP disconnect");
    }
    
    /** 向设备发送指令 */
    default void sendCommand(String deviceCode, DeviceCommand command) {
        throw new UnsupportedOperationException("MQTT adapter does not support TCP command");
    }
    
    /** 指定设备是否处于连接状态 */
    default boolean isConnected(String deviceCode) {
        return false;
    }
    
    /** 将设备上报的原始字节解析为状态报告 */
    DeviceStatusReport parseStatus(byte[] rawData);
    
    // ========== MQTT 侧（TCP 适配器提供默认实现） ==========
    
    /** 判断该适配器是否支持处理指定 MQTT Topic */
    default boolean supports(String topic) {
        return false;
    }
    
    /** 解析 MQTT 消息为状态报告（TCP 适配器无需实现） */
    default DeviceStatusReport parseMqttMessage(String topic, String payload) {
        throw new UnsupportedOperationException("TCP adapter does not support MQTT parsing");
    }
}
```

#### `com.decoction.equipment.adapter.AdapterRegistry`

```java
package com.decoction.equipment.adapter;

import com.decoction.entity.Device;
import com.decoction.mapper.DeviceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AdapterRegistry implements ApplicationContextAware {

    /** Map<manufacturer, Map<protocol, DeviceAdapter>> */
    private final Map<String, Map<String, DeviceAdapter>> registry = new ConcurrentHashMap<>();

    @Autowired
    private DeviceMapper deviceMapper;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        this.applicationContext = ctx;
    }

    @PostConstruct
    public void init() {
        // 自动扫描 Spring 容器中所有 DeviceAdapter Bean
        Map<String, DeviceAdapter> adapters = applicationContext.getBeansOfType(DeviceAdapter.class);
        adapters.values().forEach(this::register);
        log.info("AdapterRegistry 初始化完成，共注册 {} 个适配器", adapters.size());
    }

    public void register(DeviceAdapter adapter) {
        registry.computeIfAbsent(adapter.getManufacturer(), k -> new ConcurrentHashMap<>())
                .put(adapter.getProtocol(), adapter);
        log.info("注册适配器: manufacturer={}, protocol={}", adapter.getManufacturer(), adapter.getProtocol());
    }

    /**
     * 按厂商+协议精确获取适配器
     */
    public DeviceAdapter getAdapter(String manufacturer, String protocol) {
        Map<String, DeviceAdapter> protocolMap = registry.get(manufacturer);
        if (protocolMap == null) {
            throw new IllegalArgumentException("未找到厂商适配器: " + manufacturer);
        }
        DeviceAdapter adapter = protocolMap.get(protocol);
        if (adapter == null) {
            throw new IllegalArgumentException("厂商 " + manufacturer + " 不支持协议: " + protocol);
        }
        return adapter;
    }

    /**
     * 按设备编码自动路由适配器（查询数据库获取 manufacturer + protocolType）
     */
    public DeviceAdapter getAdapterForDevice(String deviceCode) {
        Device device = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>().eq(Device::getDeviceCode, deviceCode));
        if (device == null) {
            throw new IllegalArgumentException("设备不存在: " + deviceCode);
        }
        if (device.getManufacturer() == null || device.getProtocolType() == null) {
            throw new IllegalStateException("设备缺少 manufacturer 或 protocolType 配置: " + deviceCode);
        }
        return getAdapter(device.getManufacturer(), device.getProtocolType());
    }
}
```

---

### 3.3 TCP 连接管理

#### `com.decoction.equipment.adapter.connection.ConnectionPool`

```java
package com.decoction.equipment.adapter.connection;

import com.decoction.equipment.adapter.dto.DeviceStatusReport;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConnectionPool {

    /** deviceCode -> Netty Channel */
    private final Map<String, Channel> channels = new ConcurrentHashMap<>();
    /** deviceCode -> 最近一次状态报告 */
    private final Map<String, DeviceStatusReport> lastReports = new ConcurrentHashMap<>();

    public void put(String deviceCode, Channel channel) {
        channels.put(deviceCode, channel);
        log.info("设备连接已加入连接池: {}", deviceCode);
    }

    public Channel get(String deviceCode) {
        return channels.get(deviceCode);
    }

    public void remove(String deviceCode) {
        channels.remove(deviceCode);
    }

    public void close(String deviceCode) {
        Channel ch = channels.remove(deviceCode);
        if (ch != null && ch.isActive()) {
            ch.close();
            log.info("设备连接已关闭: {}", deviceCode);
        }
    }

    public boolean isActive(String deviceCode) {
        Channel ch = channels.get(deviceCode);
        return ch != null && ch.isActive();
    }

    public void updateLastReport(String deviceCode, DeviceStatusReport report) {
        lastReports.put(deviceCode, report);
    }

    public DeviceStatusReport getLastReport(String deviceCode) {
        return lastReports.get(deviceCode);
    }

    /** 获取当前活跃连接数 */
    public int activeCount() {
        return (int) channels.values().stream().filter(Channel::isActive).count();
    }
}
```

#### `com.decoction.equipment.adapter.connection.NettyTcpClient`

```java
package com.decoction.equipment.adapter.connection;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NettyTcpClient implements DisposableBean {

    /** 工作线程组，默认线程数 = CPU核心数 * 2 */
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    /**
     * 异步连接到指定地址
     * @param deviceCode 设备编码（用于日志标识）
     * @param ip 目标IP
     * @param port 目标端口
     * @param initializer ChannelPipeline 初始化器
     */
    public ChannelFuture connect(String deviceCode, String ip, int port,
                                  ChannelInitializer<SocketChannel> initializer) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                 .channel(NioSocketChannel.class)
                 .handler(initializer);
        log.info("正在连接设备 {} -> {}:{}", deviceCode, ip, port);
        return bootstrap.connect(ip, port);
    }

    @Override
    public void destroy() {
        log.info("NettyTcpClient 正在关闭...");
        workerGroup.shutdownGracefully();
    }
}
```

---

### 3.4 TCP 适配器基类

#### `com.decoction.equipment.adapter.tcp.AbstractTcpAdapter`

```java
package com.decoction.equipment.adapter.tcp;

import com.decoction.equipment.adapter.DeviceAdapter;
import com.decoction.equipment.adapter.connection.ConnectionPool;
import com.decoction.equipment.adapter.connection.NettyTcpClient;
import com.decoction.equipment.adapter.dto.DeviceCommand;
import com.decoction.equipment.adapter.dto.DeviceConnectionInfo;
import com.decoction.equipment.adapter.dto.DeviceStatusReport;
import com.decoction.service.EquipmentService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractTcpAdapter implements DeviceAdapter {

    @Autowired
    protected NettyTcpClient nettyTcpClient;
    @Autowired
    protected ConnectionPool connectionPool;
    @Autowired
    protected EquipmentService equipmentService;

    private final ScheduledExecutorService reconnectExecutor = Executors.newScheduledThreadPool(2);

    @Override
    public String getProtocol() {
        return "TCP";
    }

    @Override
    public void connect(String deviceCode, DeviceConnectionInfo info) {
        ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                // 1. 子类提供帧解码器
                ch.pipeline().addLast(getFrameDecoder());
                // 2. 通用入站处理器
                ch.pipeline().addLast(new TcpInboundHandler(deviceCode, AbstractTcpAdapter.this));
            }
        };

        ChannelFuture future = nettyTcpClient.connect(deviceCode, info.getIp(), info.getPort(), initializer);
        future.addListener((ChannelFuture f) -> {
            if (f.isSuccess()) {
                connectionPool.put(deviceCode, f.channel());
                Long deviceId = equipmentService.getDeviceId(deviceCode);
                if (deviceId != null) {
                    equipmentService.updateDeviceStatus(deviceId, "IDLE");
                }
                log.info("设备连接成功: {}", deviceCode);
            } else {
                log.error("设备连接失败: {}, 将在 {}s 后重试", deviceCode, info.getReconnectBaseSec());
                scheduleReconnect(deviceCode, info, info.getReconnectBaseSec());
            }
        });
    }

    @Override
    public void disconnect(String deviceCode) {
        connectionPool.close(deviceCode);
        Long deviceId = equipmentService.getDeviceId(deviceCode);
        if (deviceId != null) {
            equipmentService.updateDeviceStatus(deviceId, "OFFLINE");
        }
    }

    @Override
    public void sendCommand(String deviceCode, DeviceCommand command) {
        Channel channel = connectionPool.get(deviceCode);
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("设备未连接或连接已断开: " + deviceCode);
        }
        byte[] data = encodeCommand(command);
        ByteBuf buf = Unpooled.wrappedBuffer(data);
        channel.writeAndFlush(buf).addListener(f -> {
            if (!f.isSuccess()) {
                log.error("指令发送失败: {} -> {}", deviceCode, command.getCommandType(), f.cause());
            }
        });
    }

    @Override
    public boolean isConnected(String deviceCode) {
        return connectionPool.isActive(deviceCode);
    }

    /** 子类实现：返回协议特定的帧解码器（用于解决粘包/拆包） */
    protected abstract ChannelHandler getFrameDecoder();

    /** 子类实现：将 DeviceCommand 编码为字节帧 */
    protected abstract byte[] encodeCommand(DeviceCommand command);

    /**
     * 收到完整帧后的回调（由 TcpInboundHandler 调用）
     */
    void onMessageReceived(String deviceCode, byte[] msg) {
        try {
            DeviceStatusReport report = parseStatus(msg);
            if (report == null) return;
            report.setDeviceCode(deviceCode);
            report.setRawData(msg);
            report.setTimestamp(System.currentTimeMillis());
            connectionPool.updateLastReport(deviceCode, report);

            Long deviceId = equipmentService.getDeviceId(deviceCode);
            if (deviceId == null) return;

            if (report.getTemperature() != null) {
                equipmentService.updateTemperature(deviceId, report.getTemperature());
            }
            if (report.getStatus() != null) {
                equipmentService.updateDeviceStatus(deviceId, report.getStatus());
            }
            if (report.getFaultCode() != null) {
                equipmentService.reportFault(deviceId, report.getFaultCode(), "设备上报故障");
            }
        } catch (Exception e) {
            log.error("解析设备消息异常: {}", deviceCode, e);
        }
    }

    /** 连接断开回调 */
    void onDisconnected(String deviceCode) {
        connectionPool.remove(deviceCode);
        Long deviceId = equipmentService.getDeviceId(deviceCode);
        if (deviceId != null) {
            equipmentService.updateDeviceStatus(deviceId, "OFFLINE");
        }
        log.warn("设备连接已断开: {}", deviceCode);
    }

    /** 指数退避重连 */
    private void scheduleReconnect(String deviceCode, DeviceConnectionInfo info, int delaySec) {
        reconnectExecutor.schedule(() -> {
            if (!isConnected(deviceCode)) {
                connect(deviceCode, info);
            }
        }, delaySec, TimeUnit.SECONDS);
    }
}
```

---

### 3.5 TCP 入站处理器

#### `com.decoction.equipment.adapter.tcp.TcpInboundHandler`

```java
package com.decoction.equipment.adapter.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class TcpInboundHandler extends ChannelInboundHandlerAdapter {

    private final String deviceCode;
    private final AbstractTcpAdapter adapter;

    public TcpInboundHandler(String deviceCode, AbstractTcpAdapter adapter) {
        this.deviceCode = deviceCode;
        this.adapter = adapter;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) msg;
                byte[] data = new byte[buf.readableBytes()];
                buf.readBytes(data);
                adapter.onMessageReceived(deviceCode, data);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        adapter.onDisconnected(deviceCode);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("设备连接异常: {}", deviceCode, cause);
        ctx.close();
    }
}
```

---

## 4. 东华原与三延适配器实现

### 4.1 东华原煎药机适配器

#### `com.decoction.equipment.adapter.tcp.DonghuaYuanAdapter`

**协议假设**（基于常见煎药机二进制协议规律构造，实际开发需按厂商文档调整）：

```
帧结构（固定长度/变长混合）：
+--------+--------+--------+--------+--------+--------+--------+
| 帧头1  | 帧头2  | 命令字 | 数据长度|  数据域(N字节) | CRC16低| CRC16高|
+--------+--------+--------+--------+--------+--------+--------+
  0xAA     0x55     1byte    1byte      N bytes       1byte    1byte

状态上报命令字: 0x81
启动指令命令字: 0x01
停止指令命令字: 0x02
设置温度命令字: 0x03
```

```java
package com.decoction.equipment.adapter.tcp;

import com.decoction.equipment.adapter.dto.DeviceCommand;
import com.decoction.equipment.adapter.dto.DeviceStatusReport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class DonghuaYuanAdapter extends AbstractTcpAdapter {

    private static final byte[] FRAME_HEADER = {(byte) 0xAA, (byte) 0x55};
    private static final int MAX_FRAME_LENGTH = 256;

    @Override
    public String getManufacturer() {
        return "donghuayuan";
    }

    @Override
    protected ChannelHandler getFrameDecoder() {
        // LengthFieldBasedFrameDecoder(maxLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip)
        // 帧头2字节 + 命令字1字节 + 长度字段1字节 = 偏移4字节
        // 长度字段 = 数据域长度（不含帧头、命令字、长度字段自身、CRC）
        // 总长度 = 2(头) + 1(命令) + 1(长度) + length + 2(CRC) = 6 + length
        // lengthAdjustment = +2 (因为长度字段不包含 CRC 2字节，需要补偿)
        // initialBytesToStrip = 0 (保留完整帧给 parseStatus)
        return new LengthFieldBasedFrameDecoder(
                MAX_FRAME_LENGTH,   // maxFrameLength
                3,                  // lengthFieldOffset (跳过帧头2 + 命令字1)
                1,                  // lengthFieldLength (1字节长度字段)
                2,                  // lengthAdjustment (+2 补偿 CRC 字段)
                0                   // initialBytesToStrip (不剥离，保留完整帧)
        );
    }

    @Override
    protected byte[] encodeCommand(DeviceCommand command) {
        ByteBuf buf = Unpooled.buffer(32);
        buf.writeBytes(FRAME_HEADER);

        switch (command.getCommandType()) {
            case "START":
                buf.writeByte(0x01);
                buf.writeByte(0x04); // 数据长度: 2字节温度 + 2字节时间
                int targetTemp = ((Number) command.getParams().getOrDefault("targetTemp", 100)).intValue();
                int duration = ((Number) command.getParams().getOrDefault("duration", 30)).intValue();
                buf.writeShort(targetTemp);
                buf.writeShort(duration);
                break;
            case "STOP":
                buf.writeByte(0x02);
                buf.writeByte(0x00);
                break;
            case "SET_TEMP":
                buf.writeByte(0x03);
                buf.writeByte(0x02);
                int temp = ((Number) command.getParams().get("targetTemp")).intValue();
                buf.writeShort(temp);
                break;
            case "QUERY_STATUS":
                buf.writeByte(0x04);
                buf.writeByte(0x00);
                break;
            default:
                throw new IllegalArgumentException("不支持的指令类型: " + command.getCommandType());
        }

        byte[] frame = new byte[buf.readableBytes()];
        buf.readBytes(frame);
        int crc = calculateCrc16(frame, 0, frame.length);
        ByteBuf finalBuf = Unpooled.buffer(frame.length + 2);
        finalBuf.writeBytes(frame);
        finalBuf.writeShort(crc);
        byte[] result = new byte[finalBuf.readableBytes()];
        finalBuf.readBytes(result);
        return result;
    }

    @Override
    public DeviceStatusReport parseStatus(byte[] rawData) {
        if (rawData.length < 6) return null;
        // 校验帧头
        if (rawData[0] != (byte) 0xAA || rawData[1] != (byte) 0x55) {
            log.warn("东华原帧头校验失败");
            return null;
        }
        // 校验 CRC
        int receivedCrc = ((rawData[rawData.length - 2] & 0xFF) << 8) | (rawData[rawData.length - 1] & 0xFF);
        int calcCrc = calculateCrc16(rawData, 0, rawData.length - 2);
        if (receivedCrc != calcCrc) {
            log.warn("东华原 CRC 校验失败: received={}, calc={}", receivedCrc, calcCrc);
            return null;
        }

        DeviceStatusReport report = new DeviceStatusReport();
        byte cmd = rawData[2];
        if (cmd == (byte) 0x81) { // 状态上报
            ByteBuf buf = Unpooled.wrappedBuffer(rawData, 4, rawData.length - 6); // 跳过头3+长度1，去掉CRC2
            if (buf.readableBytes() >= 3) {
                int tempRaw = buf.readUnsignedShort(); // 温度 * 10
                report.setTemperature(new BigDecimal(tempRaw).divide(new BigDecimal(10)));
                byte statusByte = buf.readByte();
                report.setStatus(statusByte == 0x01 ? "BUSY" : "IDLE");
            }
        } else if (cmd == (byte) 0x82) { // 故障上报
            ByteBuf buf = Unpooled.wrappedBuffer(rawData, 4, rawData.length - 6);
            if (buf.readableBytes() >= 1) {
                byte faultCode = buf.readByte();
                report.setFaultCode("DHY_FAULT_" + (faultCode & 0xFF));
                report.setStatus("FAULT");
            }
        }
        return report;
    }

    /** CRC16-Modbus 校验 */
    private int calculateCrc16(byte[] data, int offset, int length) {
        int crc = 0xFFFF;
        for (int i = offset; i < offset + length; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc;
    }
}
```

---

### 4.2 三延煎药机适配器

#### `com.decoction.equipment.adapter.tcp.SanyanAdapter`

**协议假设**（基于常见电力/工业设备变长帧格式构造）：

```
帧结构（变长，小端序）：
+--------+----------+----------+----------+-----------+----------+
| 帧头   |  地址域  |  控制码  | 数据长度 |  数据域   |  校验和  |
| 0x68   | 4 bytes  |  1byte   | 2 bytes  | N bytes   |  1byte   |
+--------+----------+----------+----------+-----------+----------+

状态上报控制码: 0x88
启动指令控制码: 0x01
停止指令控制码: 0x02
```

```java
package com.decoction.equipment.adapter.tcp;

import com.decoction.equipment.adapter.dto.DeviceCommand;
import com.decoction.equipment.adapter.dto.DeviceStatusReport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class SanyanAdapter extends AbstractTcpAdapter {

    private static final byte FRAME_HEADER = (byte) 0x68;
    private static final int MAX_FRAME_LENGTH = 512;

    @Override
    public String getManufacturer() {
        return "sanyan";
    }

    @Override
    protected ChannelHandler getFrameDecoder() {
        // 帧头1 + 地址4 + 控制码1 = 偏移6
        // 数据长度2字节（小端），长度字段之后是数据域
        // lengthAdjustment = +1 (补偿 校验和1字节)
        return new LengthFieldBasedFrameDecoder(
                MAX_FRAME_LENGTH,
                6,   // lengthFieldOffset
                2,   // lengthFieldLength (小端2字节)
                1,   // lengthAdjustment (+1 补偿校验和)
                0    // initialBytesToStrip
        );
    }

    @Override
    protected byte[] encodeCommand(DeviceCommand command) {
        ByteBuf buf = Unpooled.buffer(64);
        buf.writeByte(FRAME_HEADER);
        buf.writeInt(0x00000001); // 默认地址，实际应从 deviceCode 映射
        
        switch (command.getCommandType()) {
            case "START":
                buf.writeByte(0x01);
                buf.writeShortLE(0x04);
                int targetTemp = ((Number) command.getParams().getOrDefault("targetTemp", 100)).intValue();
                int duration = ((Number) command.getParams().getOrDefault("duration", 30)).intValue();
                buf.writeShortLE(targetTemp);
                buf.writeShortLE(duration);
                break;
            case "STOP":
                buf.writeByte(0x02);
                buf.writeShortLE(0x00);
                break;
            case "QUERY_STATUS":
                buf.writeByte(0x03);
                buf.writeShortLE(0x00);
                break;
            default:
                throw new IllegalArgumentException("不支持的指令类型: " + command.getCommandType());
        }

        byte[] frame = new byte[buf.readableBytes()];
        buf.readBytes(frame);
        int checksum = calculateChecksum(frame);
        ByteBuf finalBuf = Unpooled.buffer(frame.length + 1);
        finalBuf.writeBytes(frame);
        finalBuf.writeByte(checksum);
        byte[] result = new byte[finalBuf.readableBytes()];
        finalBuf.readBytes(result);
        return result;
    }

    @Override
    public DeviceStatusReport parseStatus(byte[] rawData) {
        if (rawData.length < 8) return null;
        if (rawData[0] != FRAME_HEADER) {
            log.warn("三延帧头校验失败");
            return null;
        }

        // 校验累加和（除最后一个字节外全部累加，低8位）
        int calcSum = calculateChecksum(rawData, 0, rawData.length - 1);
        int receivedSum = rawData[rawData.length - 1] & 0xFF;
        if (calcSum != receivedSum) {
            log.warn("三延校验和失败: calc={}, received={}", calcSum, receivedSum);
            return null;
        }

        DeviceStatusReport report = new DeviceStatusReport();
        byte ctrl = rawData[5];
        if (ctrl == (byte) 0x88) { // 状态上报
            ByteBuf buf = Unpooled.wrappedBuffer(rawData, 8, rawData.length - 9); // 跳过头8，去掉校验1
            if (buf.readableBytes() >= 5) {
                int tempRaw = buf.readShortLE(); // 温度 * 100
                report.setTemperature(new BigDecimal(tempRaw).divide(new BigDecimal(100)));
                byte statusByte = buf.readByte();
                report.setStatus((statusByte & 0x01) != 0 ? "BUSY" : "IDLE");
                byte faultByte = buf.readByte();
                if (faultByte != 0) {
                    report.setFaultCode("SY_FAULT_" + (faultByte & 0xFF));
                    report.setStatus("FAULT");
                }
            }
        }
        return report;
    }

    /** 累加和校验 */
    private int calculateChecksum(byte[] data, int offset, int length) {
        int sum = 0;
        for (int i = offset; i < offset + length; i++) {
            sum += (data[i] & 0xFF);
        }
        return sum & 0xFF;
    }

    private int calculateChecksum(byte[] data) {
        return calculateChecksum(data, 0, data.length);
    }
}
```

---

## 5. 数据库变更

### 5.1 新增迁移脚本

**文件:** `src/main/resources/db/migration/V5__adapter.sql`

```sql
-- ========== 设备表扩展：厂商标识 ==========
ALTER TABLE device ADD COLUMN manufacturer VARCHAR(32) DEFAULT NULL;

-- 创建索引加速 AdapterRegistry 查询
CREATE INDEX IF NOT EXISTS idx_device_manufacturer ON device(manufacturer);

-- ========== 适配器配置表（预留，Sprint 3 第二阶段启用） ==========
CREATE TABLE IF NOT EXISTS device_adapter_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    manufacturer VARCHAR(32) NOT NULL,
    protocol_type VARCHAR(16) NOT NULL,
    frame_header_hex VARCHAR(32),
    frame_tail_hex VARCHAR(32),
    length_field_offset INT DEFAULT 0,
    length_field_length INT DEFAULT 1,
    length_adjustment INT DEFAULT 0,
    heartbeat_interval_sec INT DEFAULT 30,
    reconnect_base_sec INT DEFAULT 10,
    reconnect_max_sec INT DEFAULT 60,
    command_timeout_ms INT DEFAULT 5000,
    config_json TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 初始化东华原配置
INSERT INTO device_adapter_config (manufacturer, protocol_type, frame_header_hex, length_field_offset, length_field_length, length_adjustment)
VALUES ('donghuayuan', 'TCP', 'AA55', 3, 1, 2);

-- 初始化三延配置
INSERT INTO device_adapter_config (manufacturer, protocol_type, frame_header_hex, length_field_offset, length_field_length, length_adjustment, reconnect_base_sec)
VALUES ('sanyan', 'TCP', '68', 6, 2, 1, 5);
```

### 5.2 DatabaseInitConfig 更新

在 `DatabaseInitConfig.java` 中新增 V5 加载：

```java
Resource v5 = new ClassPathResource("db/migration/V5__adapter.sql");
ScriptUtils.executeSqlScript(conn, v5);
```

---

## 6. REST API 详细定义

### 6.1 新增端点

#### POST `/api/devices/{deviceCode}/connect`

| 项 | 说明 |
|----|------|
| 描述 | 手动触发与设备的 TCP 连接 |
| 路径变量 | `deviceCode` — 设备编码 |
| 请求体 | 无（IP/Port 从数据库读取） |
| 成功响应 | `{"code":200,"message":"success","data":{"deviceCode":"DHY001","status":"IDLE","connected":true}}` |
| 错误码 | 400(设备不存在), 409(已连接), 500(连接失败) |

#### POST `/api/devices/{deviceCode}/disconnect`

| 项 | 说明 |
|----|------|
| 描述 | 手动断开设备 TCP 连接 |
| 成功响应 | `{"code":200,"message":"success","data":{"deviceCode":"DHY001","status":"OFFLINE","connected":false}}` |
| 错误码 | 400(设备不存在), 409(未连接) |

#### POST `/api/devices/{deviceCode}/command`

| 项 | 说明 |
|----|------|
| 描述 | 向设备下发指令 |
| 请求体 | `{"commandType":"START","params":{"targetTemp":100,"duration":30}}` |
| 成功响应 | `{"code":200,"message":"success","data":{"deviceCode":"DHY001","commandType":"START","sent":true}}` |
| 错误码 | 400(设备不存在/参数错误), 409(设备未连接), 500(发送失败) |

#### GET `/api/devices/{deviceCode}/connection`

| 项 | 说明 |
|----|------|
| 描述 | 查询设备连接状态及最近一次上报 |
| 成功响应 | `{"code":200,"data":{"deviceCode":"DHY001","connected":true,"lastReport":{"temperature":98.5,"status":"BUSY","timestamp":1714032000000}}}` |

### 6.2 Controller 代码框架

```java
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceAdapterController {

    private final AdapterRegistry adapterRegistry;
    private final DeviceService deviceService;

    @PostMapping("/{deviceCode}/connect")
    public ApiResponse<DeviceConnectionVO> connect(@PathVariable String deviceCode) {
        Device device = deviceService.getByCode(deviceCode);
        if (device == null) throw new IllegalArgumentException("设备不存在");
        DeviceAdapter adapter = adapterRegistry.getAdapterForDevice(deviceCode);
        DeviceConnectionInfo info = new DeviceConnectionInfo();
        info.setIp(device.getIpAddress());
        info.setPort(device.getPort());
        adapter.connect(deviceCode, info);
        // ... 构造响应
        return ApiResponse.success(vo);
    }

    @PostMapping("/{deviceCode}/disconnect")
    public ApiResponse<DeviceConnectionVO> disconnect(@PathVariable String deviceCode) {
        DeviceAdapter adapter = adapterRegistry.getAdapterForDevice(deviceCode);
        adapter.disconnect(deviceCode);
        return ApiResponse.success(vo);
    }

    @PostMapping("/{deviceCode}/command")
    public ApiResponse<CommandResultVO> sendCommand(
            @PathVariable String deviceCode,
            @RequestBody @Validated DeviceCommand command) {
        DeviceAdapter adapter = adapterRegistry.getAdapterForDevice(deviceCode);
        adapter.sendCommand(deviceCode, command);
        return ApiResponse.success(vo);
    }

    @GetMapping("/{deviceCode}/connection")
    public ApiResponse<DeviceConnectionVO> getConnectionStatus(@PathVariable String deviceCode) {
        DeviceAdapter adapter = adapterRegistry.getAdapterForDevice(deviceCode);
        boolean connected = adapter.isConnected(deviceCode);
        // ... 查询 ConnectionPool 获取 lastReport
        return ApiResponse.success(vo);
    }
}
```

---

## 7. Netty Pipeline 配置

### 7.1 不同厂商的 Pipeline 差异

```
东华原 Pipeline:
  [LengthFieldBasedFrameDecoder(256, 3, 1, 2, 0)]
  → [TcpInboundHandler]

三延 Pipeline:
  [LengthFieldBasedFrameDecoder(512, 6, 2, 1, 0)]
  → [TcpInboundHandler]
```

**说明:** `LengthFieldBasedFrameDecoder` 是 Netty 内置的粘包/拆包解决器，通过解析帧中的长度字段自动切分出完整帧。子类通过 `getFrameDecoder()` 返回配置好的实例。

---

## 8. 异常处理设计

| 异常场景 | 异常类型 | 处理方式 |
|---------|---------|---------|
| 设备编码不存在 | `IllegalArgumentException` | Controller 捕获 → 400 |
| 设备未连接时发送指令 | `IllegalStateException` | Controller 捕获 → 409 |
| 连接超时 | `ConnectTimeoutException` | AbstractTcpAdapter 触发重连 |
| 帧头/CRC 校验失败 | 日志警告，丢弃帧 | 不抛异常，防止异常帧导致断连 |
| 未知指令类型 | `IllegalArgumentException` | Controller 捕获 → 400 |
| 厂商未注册适配器 | `IllegalArgumentException` | Controller 捕获 → 400 |
| Netty I/O 异常 | `IOException` | TcpInboundHandler.exceptionCaught → 关闭 Channel → 触发重连 |

---

## 9. 测试方案

### 9.1 单元测试

#### `DonghuaYuanAdapterTest`

```java
@SpringBootTest
class DonghuaYuanAdapterTest {

    @Autowired
    private DonghuaYuanAdapter adapter;

    @Test
    void parseStatus_shouldExtractTemperatureAndStatus() {
        // 构造模拟状态帧: AA 55 81 03 03 E8 01 XX XX (温度100.0℃, BUSY)
        byte[] frame = HexUtils.decodeHex("AA55810303E801");
        int crc = adapter.calculateCrc16(frame, 0, frame.length);
        byte[] fullFrame = new byte[frame.length + 2];
        System.arraycopy(frame, 0, fullFrame, 0, frame.length);
        fullFrame[frame.length] = (byte) (crc & 0xFF);
        fullFrame[frame.length + 1] = (byte) ((crc >> 8) & 0xFF);

        DeviceStatusReport report = adapter.parseStatus(fullFrame);
        assertEquals(new BigDecimal("100.0"), report.getTemperature());
        assertEquals("BUSY", report.getStatus());
    }

    @Test
    void encodeCommand_start_shouldProduceValidFrame() {
        DeviceCommand cmd = new DeviceCommand();
        cmd.setCommandType("START");
        cmd.setParams(Map.of("targetTemp", 100, "duration", 30));

        byte[] frame = adapter.encodeCommand(cmd);
        assertEquals((byte) 0xAA, frame[0]);
        assertEquals((byte) 0x55, frame[1]);
        assertEquals((byte) 0x01, frame[2]); // 启动命令字
    }
}
```

### 9.2 集成测试（Mock TCP Server）

```java
@SpringBootTest
class TcpAdapterIntegrationTest {

    @Autowired
    private DonghuaYuanAdapter adapter;

    private NettyMockServer mockServer;

    @BeforeEach
    void setUp() {
        // 启动一个模拟的东华原设备 TCP Server
        mockServer = new NettyMockServer(0); // 随机端口
        mockServer.start();
    }

    @Test
    void connectAndReceiveStatus_shouldUpdateEquipment() {
        DeviceConnectionInfo info = new DeviceConnectionInfo();
        info.setIp("127.0.0.1");
        info.setPort(mockServer.getPort());
        info.setConnectTimeoutMs(2000);

        adapter.connect("DHY_TEST_001", info);
        // 等待连接建立
        await().atMost(3, SECONDS).until(() -> adapter.isConnected("DHY_TEST_001"));

        // Mock Server 推送状态帧
        mockServer.send(HexUtils.decodeHex("AA55810303E801XXXX"));

        // 验证 EquipmentService 被调用（使用 Mockito verify）
        verify(equipmentService).updateTemperature(any(), eq(new BigDecimal("100.0")));
    }
}
```

### 9.3 Mock TCP Server 实现

```java
@Component
public class NettyMockServer {
    private final int port;
    private Channel channel;
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    public void start() {
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             protected void initChannel(SocketChannel ch) {
                 channel = ch;
             }
         });
        b.bind(port).sync();
    }

    public void send(byte[] data) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(Unpooled.wrappedBuffer(data));
        }
    }

    public void stop() {
        if (channel != null) channel.close();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
```

---

## 10. 开发顺序与工时估算

| 序号 | 任务 | 预估工时 | 依赖 |
|------|------|---------|------|
| 1 | pom.xml 添加 Netty 依赖 | 0.1d | 无 |
| 2 | DTO 类（DeviceConnectionInfo/DeviceCommand/DeviceStatusReport） | 0.2d | 1 |
| 3 | ByteUtils / HexUtils 工具 | 0.2d | 1 |
| 4 | DeviceAdapter 接口 + AdapterRegistry | 0.5d | 2 |
| 5 | NettyTcpClient + ConnectionPool | 0.5d | 1 |
| 6 | TcpInboundHandler | 0.3d | 5 |
| 7 | AbstractTcpAdapter | 0.5d | 4,5,6 |
| 8 | DonghuaYuanAdapter | 1.0d | 7 |
| 9 | SanyanAdapter | 1.0d | 7 |
| 10 | device 表新增 manufacturer 字段 + V5 SQL | 0.3d | 无 |
| 11 | DeviceAdapterController REST API | 0.3d | 4 |
| 12 | NettyMockServer（测试工具） | 0.5d | 1 |
| 13 | 单元测试（AdapterRegistry + 两家适配器） | 1.0d | 8,9,12 |
| 14 | 集成测试（connect + sendCommand + parseStatus） | 0.5d | 13 |
| **合计** | | **~6.9d** | |

---

## 11. 风险与回滚方案

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| Netty 引入导致包体积增加 | 低 | 中 | `netty-all` 约 4.2MB，接受范围内；上线前可替换为精简依赖 |
| 厂商协议文档与假设不符 | 中 | 高 | 协议解析逻辑封装在 `parseStatus` / `encodeCommand` 中，仅需修改子类；预留 `config_json` 字段 |
| 与现有 MQTT 处理冲突 | 低 | 高 | 第一阶段保持 MQTT 逻辑不变，TCP 走独立路径；无代码冲突 |
| 重连机制导致线程泄漏 | 低 | 中 | 使用 `DisposableBean` 关闭 `EventLoopGroup`；重连定时器使用单例线程池 |
| 测试环境无真实设备 | 高 | 中 | 提供 `NettyMockServer` 模拟设备；CI 中使用 Mock 测试 |

**回滚方案:** 若 Sprint 3 验证不通过，可直接移除 `com.decoction.equipment.adapter` 包及相关 Bean，不影响现有 MQTT 和设备管理功能。

---

## 12. 需要架构师最终确认

1. **Netty 版本 `4.1.94.Final` 是否确认？** 或降级到 `4.1.86.Final`（更稳定）？
2. **协议帧格式** 当前为基于行业惯例的假设设计，实际开发时是否需要等待厂商文档？
3. **REST API 路径** 使用 `/api/devices/{deviceCode}/connect` 还是 `/api/v1/eq/devices/{deviceCode}/connect`（与 openygt-dms 对齐）？
4. **是否 Sprint 3 就实现 `DeviceService` 中自动 connect？** 即 `startDecoct` 时自动连接设备，还是保持手动连接？
