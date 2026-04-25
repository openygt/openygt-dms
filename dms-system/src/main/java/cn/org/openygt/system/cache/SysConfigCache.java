package cn.org.openygt.system.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 系统配置本地缓存 —— 带缓存穿透防护（互斥锁 + 空值缓存）。
 *
 * <p>当前为 JVM 本地缓存，集群部署后需替换为 Redis/Caffeine。</p>
 */
@Slf4j
@Component
public class SysConfigCache {

    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    /**
     * 空值占位符（缓存穿透防护）。
     */
    private static final Object NULL_VALUE = new Object();

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Function<String, T> loader) {
        Object value = cache.get(key);
        if (value != null) {
            return value == NULL_VALUE ? null : (T) value;
        }

        // 互斥锁防止缓存击穿
        Object lock = locks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            value = cache.get(key);
            if (value != null) {
                return value == NULL_VALUE ? null : (T) value;
            }

            T loaded = loader.apply(key);
            if (loaded != null) {
                cache.put(key, loaded);
            } else {
                cache.put(key, NULL_VALUE);
            }
            return loaded;
        }
    }

    public void invalidate(String key) {
        if (key != null) {
            cache.remove(key);
            log.debug("配置缓存已失效: {}", key);
        }
    }

    public void invalidateAll() {
        cache.clear();
        log.info("配置缓存已全部清空");
    }
}
