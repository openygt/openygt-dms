package cn.org.openygt.system.service.impl;

import cn.org.openygt.system.cache.SysConfigCache;
import cn.org.openygt.system.entity.SysConfig;
import cn.org.openygt.system.mapper.SysConfigMapper;
import cn.org.openygt.system.service.SysConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SysConfigServiceImpl 单元测试 —— CRUD + 缓存一致性。
 */
@ExtendWith(MockitoExtension.class)
class SysConfigServiceImplTest {

    @Mock
    private SysConfigMapper configMapper;

    @Mock
    private SysConfigCache configCache;

    private SysConfigServiceImpl configService;

    @BeforeEach
    void setUp() {
        configService = new SysConfigServiceImpl(configMapper, configCache);
    }

    // ---- create ----

    @Test
    void create_shouldInsertAndInvalidateCache() {
        SysConfig config = new SysConfig();
        config.setConfigKey("test.key");
        config.setConfigValue("test-value");

        when(configMapper.insert(any(SysConfig.class))).thenReturn(1);

        SysConfig result = configService.create(config);

        assertNotNull(result);
        verify(configMapper).insert(config);
        verify(configCache).invalidate("test.key");
    }

    // ---- update ----

    @Test
    void update_shouldSucceed_whenExists() {
        SysConfig existing = new SysConfig();
        existing.setId(1L);
        existing.setConfigKey("old.key");
        existing.setConfigValue("old-value");

        SysConfig update = new SysConfig();
        update.setConfigKey("new.key");
        update.setConfigValue("new-value");

        when(configMapper.selectById(anyLong())).thenReturn(existing).thenReturn(update);
        when(configMapper.updateById(any(SysConfig.class))).thenReturn(1);

        SysConfig result = configService.update(1L, update);

        assertNotNull(result);
        assertEquals("new.key", result.getConfigKey());
        verify(configCache).invalidate("old.key");
        verify(configCache).invalidate("new.key");
    }

    @Test
    void update_shouldThrow_whenNotExists() {
        SysConfig update = new SysConfig();
        update.setConfigKey("key");
        update.setConfigValue("value");

        when(configMapper.selectById(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> configService.update(1L, update));
        verify(configCache, never()).invalidate(any());
    }

    // ---- getById ----

    @Test
    void getById_shouldReturnConfig_whenExists() {
        SysConfig config = new SysConfig();
        config.setId(1L);
        config.setConfigKey("key");
        config.setConfigValue("value");

        when(configMapper.selectById(1L)).thenReturn(config);

        SysConfig result = configService.getById(1L);

        assertNotNull(result);
        assertEquals("key", result.getConfigKey());
    }

    @Test
    void getById_shouldThrow_whenNotExists() {
        when(configMapper.selectById(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> configService.getById(1L));
    }

    // ---- getByKey / getStringValue / getIntValue ----

    @Test
    void getByKey_shouldHitCache() {
        SysConfig config = new SysConfig();
        config.setConfigKey("my.key");
        config.setConfigValue("my-value");

        when(configCache.get(eq("my.key"), any())).thenAnswer(inv -> {
            java.util.function.Function<String, SysConfig> loader = inv.getArgument(1);
            return loader.apply("my.key");
        });
        when(configMapper.selectOne(any())).thenReturn(config);

        SysConfig result = configService.getByKey("my.key");

        assertNotNull(result);
        assertEquals("my-value", result.getConfigValue());
    }

    @Test
    void getStringValue_shouldReturnDefault_whenNotExists() {
        when(configCache.get(eq("missing.key"), any())).thenAnswer(inv -> {
            java.util.function.Function<String, SysConfig> loader = inv.getArgument(1);
            return loader.apply("missing.key");
        });
        when(configMapper.selectOne(any())).thenReturn(null);

        String result = configService.getStringValue("missing.key", "default-val");

        assertEquals("default-val", result);
    }

    @Test
    void getIntValue_shouldReturnParsedInt() {
        SysConfig config = new SysConfig();
        config.setConfigKey("timeout");
        config.setConfigValue("30");

        when(configCache.get(eq("timeout"), any())).thenAnswer(inv -> {
            java.util.function.Function<String, SysConfig> loader = inv.getArgument(1);
            return loader.apply("timeout");
        });
        when(configMapper.selectOne(any())).thenReturn(config);

        Integer result = configService.getIntValue("timeout", 10);

        assertEquals(30, result);
    }

    @Test
    void getIntValue_shouldReturnDefault_whenParseFails() {
        SysConfig config = new SysConfig();
        config.setConfigKey("bad");
        config.setConfigValue("not-a-number");

        when(configCache.get(eq("bad"), any())).thenAnswer(inv -> {
            java.util.function.Function<String, SysConfig> loader = inv.getArgument(1);
            return loader.apply("bad");
        });
        when(configMapper.selectOne(any())).thenReturn(config);

        Integer result = configService.getIntValue("bad", 42);

        assertEquals(42, result);
    }

    // ---- delete ----

    @Test
    void delete_shouldEvictCache_whenExists() {
        SysConfig existing = new SysConfig();
        existing.setId(1L);
        existing.setConfigKey("delete.key");

        when(configMapper.selectById(1L)).thenReturn(existing);

        configService.delete(1L);

        verify(configCache).invalidate("delete.key");
        verify(configMapper).deleteById(1L);
    }

    @Test
    void delete_shouldNotEvict_whenNotExists() {
        when(configMapper.selectById(1L)).thenReturn(null);

        configService.delete(1L);

        verify(configCache, never()).invalidate(any());
        verify(configMapper).deleteById(1L);
    }
}
