package cn.org.openygt.iot.adapter;

import cn.org.openygt.iot.protocol.DeviceMessage;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * TCP 二进制协议适配器抽象基类
 *
 * <p>新延等 TCP 二进制设备厂商继承此类，实现帧解码和编码逻辑。</p>
 *
 * <p>厂商实现必须位于私有仓库中；开源仓库仅保留此基类。</p>
 */
@Slf4j
public abstract class AbstractTcpBinaryAdapter implements DeviceAdapter {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    protected AdapterStatus status = AdapterStatus.INITIALIZED;

    /** 子类定义监听端口 */
    protected abstract int getPort();

    /** 子类定义 ChannelInitializer（包含帧解码器、业务 Handler） */
    protected abstract ChannelInitializer<SocketChannel> getChannelInitializer();

    /** 子类实现：将指令编码为二进制帧 */
    protected abstract byte[] encodeCommand(DeviceCommandDTO command);

    /** 子类实现：二进制数据解码为统一消息 */
    protected abstract DeviceMessage decodeData(String deviceCode, byte[] data);

    /** 数据解码后进入业务处理 —— 子类可选择性重写 */
    protected abstract void onDataReceived(String deviceCode, byte[] data);

    @Override
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(getChannelInitializer());

            ChannelFuture future = bootstrap.bind(getPort()).sync();
            serverChannel = future.channel();
            status = AdapterStatus.RUNNING;
            log.info("[{}] TCP 适配器已启动，监听端口 {}", getProtocolType(), getPort());
        } catch (InterruptedException e) {
            log.error("[{}] TCP 适配器启动失败: {}", getProtocolType(), e.getMessage(), e);
            status = AdapterStatus.ERROR;
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            status = AdapterStatus.STOPPED;
            log.info("[{}] TCP 适配器已停止", getProtocolType());
        } catch (InterruptedException e) {
            log.error("[{}] TCP 适配器停止异常: {}", getProtocolType(), e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void sendCommand(String deviceCode, DeviceCommandDTO command) {
        byte[] frame = encodeCommand(command);
        Channel channel = getDeviceChannel(deviceCode);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(frame);
            log.info("[{}] 下发指令 device={}, command={}", getProtocolType(), deviceCode, command.getCommandType());
        } else {
            log.warn("[{}] 设备未连接，无法下发指令 device={}", getProtocolType(), deviceCode);
        }
    }

    @Override
    public AdapterStatus getStatus() {
        return status;
    }

    /** 子类维护 deviceCode → Channel 映射 */
    protected abstract Channel getDeviceChannel(String deviceCode);
}
