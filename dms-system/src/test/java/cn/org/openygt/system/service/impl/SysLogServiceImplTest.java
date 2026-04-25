package cn.org.openygt.system.service.impl;

import cn.org.openygt.system.entity.SysLog;
import cn.org.openygt.system.mapper.SysLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysLogServiceImplTest {

    @Mock
    private SysLogMapper logMapper;

    private SysLogServiceImpl logService;

    @BeforeEach
    void setUp() {
        logService = new SysLogServiceImpl(logMapper);
    }

    @Test
    void saveLog_shouldInsert() {
        SysLog log = new SysLog();
        log.setAction("TestController.test");
        log.setModule("system");

        when(logMapper.insert(any(SysLog.class))).thenReturn(1);

        logService.saveLog(log);

        verify(logMapper).insert(log);
    }

    @Test
    void list_shouldReturnPagedResult() {
        Page<SysLog> page = new Page<>(1, 10);
        when(logMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        IPage<SysLog> result = logService.list("keyword", 1, 10);

        assertNotNull(result);
    }

    @Test
    void list_shouldReturnAll_whenKeywordNull() {
        Page<SysLog> page = new Page<>(1, 20);
        when(logMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        IPage<SysLog> result = logService.list(null, 1, 20);

        assertNotNull(result);
    }
}
