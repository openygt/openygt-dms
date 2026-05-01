package cn.org.openygt.system.service.impl;

import cn.org.openygt.system.entity.InterfaceLog;
import cn.org.openygt.system.mapper.InterfaceLogMapper;
import cn.org.openygt.system.service.InterfaceLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InterfaceLogServiceImpl extends ServiceImpl<InterfaceLogMapper, InterfaceLog> implements InterfaceLogService {

    @Override
    public Page<InterfaceLog> listLogs(Long interfaceId, String result, int page, int size) {
        LambdaQueryWrapper<InterfaceLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterfaceLog::getDeleted, 0);
        wrapper.orderByDesc(InterfaceLog::getCreatedAt);
        if (interfaceId != null) {
            wrapper.eq(InterfaceLog::getInterfaceId, interfaceId);
        }
        if (result != null && !result.isEmpty()) {
            wrapper.eq(InterfaceLog::getResult, result);
        }
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
