package cn.org.openygt.masterdata.service.impl;

import cn.org.openygt.masterdata.entity.DecoctScheme;
import cn.org.openygt.masterdata.mapper.DecoctSchemeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DecoctSchemeServiceImpl 单元测试 —— CRUD。
 */
@ExtendWith(MockitoExtension.class)
class DecoctSchemeServiceImplTest {

    @Mock
    private DecoctSchemeMapper schemeMapper;

    private DecoctSchemeServiceImpl schemeService;

    @BeforeEach
    void setUp() {
        schemeService = new DecoctSchemeServiceImpl(schemeMapper);
    }

    @Test
    void create_shouldInsertScheme() {
        DecoctScheme scheme = new DecoctScheme();
        scheme.setName("测试方案");

        when(schemeMapper.insert(any(DecoctScheme.class))).thenReturn(1);

        DecoctScheme result = schemeService.create(scheme);

        assertNotNull(result);
        assertEquals("测试方案", result.getName());
        verify(schemeMapper).insert(scheme);
    }

    @Test
    void update_shouldSucceed_whenExists() {
        DecoctScheme existing = new DecoctScheme();
        existing.setId(1L);
        existing.setName("旧方案");

        DecoctScheme update = new DecoctScheme();
        update.setName("新方案");

        when(schemeMapper.updateById(any(DecoctScheme.class))).thenReturn(1);
        when(schemeMapper.selectById(anyLong())).thenReturn(existing).thenReturn(update);

        DecoctScheme result = schemeService.update(1L, update);

        assertNotNull(result);
        assertEquals("新方案", result.getName());
    }

    @Test
    void update_shouldThrow_whenNotExists() {
        when(schemeMapper.selectById(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> schemeService.update(1L, new DecoctScheme()));
    }

    @Test
    void getById_shouldReturnScheme_whenExists() {
        DecoctScheme scheme = new DecoctScheme();
        scheme.setId(1L);
        scheme.setName("方案A");

        when(schemeMapper.selectById(1L)).thenReturn(scheme);

        DecoctScheme result = schemeService.getById(1L);

        assertNotNull(result);
        assertEquals("方案A", result.getName());
    }

    @Test
    void getById_shouldThrow_whenNotExists() {
        when(schemeMapper.selectById(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> schemeService.getById(1L));
    }

    @Test
    void list_shouldApplyKeywordFilter() {
        Page<DecoctScheme> pageResult = new Page<>(1, 10);
        when(schemeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

        IPage<DecoctScheme> result = schemeService.list("方案", 1, 10);

        assertNotNull(result);
        verify(schemeMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void list_shouldReturnAll_whenKeywordEmpty() {
        Page<DecoctScheme> pageResult = new Page<>(1, 20);
        when(schemeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

        IPage<DecoctScheme> result = schemeService.list(null, 1, 20);

        assertNotNull(result);
    }

    @Test
    void delete_shouldSucceed_whenExists() {
        DecoctScheme existing = new DecoctScheme();
        existing.setId(1L);
        existing.setName("方案A");
        when(schemeMapper.selectById(1L)).thenReturn(existing);

        schemeService.delete(1L);

        verify(schemeMapper).deleteById((Long) Long.valueOf(1L));
    }

    @Test
    void delete_shouldThrow_whenNotExists() {
        when(schemeMapper.selectById(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> schemeService.delete(1L));
        verify(schemeMapper, never()).deleteById(any(Long.class));
    }
}
