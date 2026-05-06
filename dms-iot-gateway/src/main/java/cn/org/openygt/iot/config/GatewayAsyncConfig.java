package cn.org.openygt.iot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 网关异步执行器配置。
 *
 * <p>为消息回写后端提供独立线程池，避免阻塞主线程且不影响设备在线状态更新。</p>
 */
@Configuration
@EnableAsync
public class GatewayAsyncConfig {

    @Bean(name = "gatewayAsyncExecutor")
    public Executor gatewayAsyncExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,                                 // 核心线程数
                8,                                 // 最大线程数
                60L,                               // 空闲线程存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),   // 任务队列
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由调用线程执行
        );
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
