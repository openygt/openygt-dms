package cn.org.openygt.system.service.impl;

import cn.org.openygt.system.entity.SysLog;
import cn.org.openygt.system.mapper.SysLogMapper;
import cn.org.openygt.system.service.SysLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SysLogServiceImpl implements SysLogService {

    private final SysLogMapper logMapper;

    @Override
    public void saveLog(SysLog log) {
        logMapper.insert(log);
    }

    @Override
    public IPage<SysLog> list(String keyword, int page, int size) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysLog::getAction, keyword)
                   .or()
                   .like(SysLog::getModule, keyword)
                   .or()
                   .like(SysLog::getDetail, keyword);
        }
        wrapper.orderByDesc(SysLog::getCreatedAt);
        return logMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
