package cn.org.openygt.masterdata.config;

import cn.org.openygt.masterdata.entity.DecoctScheme;
import cn.org.openygt.masterdata.mapper.DecoctSchemeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DecoctSchemeInitializer 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class DecoctSchemeInitializerTest {

    @Mock
    private DecoctSchemeMapper schemeMapper;

    @InjectMocks
    private DecoctSchemeInitializer initializer;

    @Test
    void testRunWhenDataExists() {
        when(schemeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(6L);

        initializer.run();

        verify(schemeMapper, never()).insert(any(DecoctScheme.class));
    }

    @Test
    void testRunWhenDataNotExists() {
        when(schemeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        initializer.run();

        ArgumentCaptor<DecoctScheme> captor = ArgumentCaptor.forClass(DecoctScheme.class);
        verify(schemeMapper, times(3)).insert(captor.capture());

        java.util.List<DecoctScheme> schemes = captor.getAllValues();
        assertEquals(3, schemes.size());

        assertEquals("常规煎药方案", schemes.get(0).getName());
        assertEquals(0, schemes.get(0).getSchemeType());
        assertEquals(1, schemes.get(0).getDecoctTimes());

        assertEquals("浓缩煎药方案", schemes.get(1).getName());
        assertEquals(1, schemes.get(1).getSchemeType());

        assertEquals("儿童轻量方案", schemes.get(2).getName());
        assertEquals("系统内置方案", schemes.get(2).getDescription());
    }
}
