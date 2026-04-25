package cn.org.openygt.equipment.adapter;

import cn.org.openygt.equipment.dto.DeviceCommandDTO;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * TCP 二进制协议适配器抽象基类 —— 基于 Netty 实现服务端监听。
 *
 * <p>东华原等厂商的 TCP 二进制适配器可继承此类实现编解码。</p>
 */
@Slf4j
public abstract class AbstractTcpBinaryAdapter implements DeviceAdapter {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @Override
    public String getProtocolType() {
        return "TCP_BINARY";
    }

    /**
     * 子类提供监听端口。
     */
    protected abstract int getPort();

    /**
     * 子类提供 ChannelInitializer，包含编解码器和业务 Handler。
     */
    protected abstract ChannelInitializer<SocketChannel> getChannelInitializer();

    @PostConstruct
    @Override
    public void connect() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(getChannelInitializer())
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind(getPort()).sync();
            serverChannel = future.channel();
            log.info("TCP binary adapter started: vendor={}, port={}", getVendor(), getPort());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("TCP adapter start interrupted", e);
        }
    }

    @PreDestroy
    @Override
    public void disconnect() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        log.info("TCP binary adapter stopped: vendor={}", getVendor());
    }

    @Override
    public void sendCommand(String deviceCode, DeviceCommandDTO command) {
        Channel channel = findChannelByDeviceCode(deviceCode);
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("设备未连接: " + deviceCode);
        }
        byte[] frame = encodeCommand(command);
        channel.writeAndFlush(frame);
    }

    /**
     * 子类实现：根据 deviceCode 查找对应 Netty Channel。
     */
    protected abstract Channel findChannelByDeviceCode(String deviceCode);

    /**
     * 子类实现：将指令编码为字节帧。
     */
    protected abstract byte[] encodeCommand(DeviceCommandDTO command);

    /**
     * 子类调用：收到原始字节数据后的统一入口。
     */
    protected abstract void onDataReceived(String deviceCode, byte[] data);
}
