package cn.org.openygt.system.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SysConfigCache 单元测试。
 */
class SysConfigCacheTest {

    private SysConfigCache cache;

    @BeforeEach
    void setUp() {
        cache = new SysConfigCache();
    }

    @Test
    void testCacheHit() {
        AtomicInteger callCount = new AtomicInteger(0);
        Function<String, String> loader = k -> {
            callCount.incrementAndGet();
            return "value-" + k;
        };

        String v1 = cache.get("key1", loader);
        String v2 = cache.get("key1", loader);

        assertEquals("value-key1", v1);
        assertEquals(v1, v2);
        assertEquals(1, callCount.get(), "loader 应只被调用一次");
    }

    @Test
    void testCacheMissWithNullValue() {
        AtomicInteger callCount = new AtomicInteger(0);
        Function<String, String> loader = k -> {
            callCount.incrementAndGet();
            return null;
        };

        String v1 = cache.get("key2", loader);
        String v2 = cache.get("key2", loader);

        assertNull(v1);
        assertNull(v2);
        assertEquals(1, callCount.get(), "loader 应只被调用一次（空值缓存防穿透）");
    }

    @Test
    void testInvalidate() {
        AtomicInteger callCount = new AtomicInteger(0);
        Function<String, String> loader = k -> {
            callCount.incrementAndGet();
            return "val";
        };

        cache.get("key3", loader);
        cache.invalidate("key3");
        cache.get("key3", loader);

        assertEquals(2, callCount.get(), "invalidate 后应重新加载");
    }

    @Test
    void testInvalidateAll() {
        AtomicInteger callCount = new AtomicInteger(0);
        Function<String, String> loader = k -> {
            callCount.incrementAndGet();
            return "val";
        };

        cache.get("k1", loader);
        cache.get("k2", loader);
        cache.invalidateAll();
        cache.get("k1", loader);
        cache.get("k2", loader);

        assertEquals(4, callCount.get(), "invalidateAll 后所有缓存应失效");
    }

    @Test
    void testDifferentKeys() {
        cache.get("a", k -> "A");
        cache.get("b", k -> "B");

        assertEquals("A", cache.get("a", k -> "X"));
        assertEquals("B", cache.get("b", k -> "Y"));
    }
}
