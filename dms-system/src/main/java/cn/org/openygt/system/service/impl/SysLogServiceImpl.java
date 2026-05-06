package cn.org.openygt.system.service.impl;

import cn.org.openygt.system.entity.SysLog;
import cn.org.openygt.system.entity.SysUser;
import cn.org.openygt.system.mapper.SysLogMapper;
import cn.org.openygt.system.mapper.SysUserMapper;
import cn.org.openygt.system.service.SysLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysLogServiceImpl implements SysLogService {

    private final SysLogMapper logMapper;
    private final SysUserMapper userMapper;

    @Override
    public void saveLog(SysLog log) {
        logMapper.insert(log);
    }

    @Override
    public IPage<SysLog> list(String keyword, String module, int page, int size) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysLog::getAction, keyword)
                   .or()
                   .like(SysLog::getDetail, keyword));
        }
        if (module != null && !module.isEmpty() && !"all".equals(module)) {
            wrapper.eq(SysLog::getModule, module);
        }
        wrapper.orderByDesc(SysLog::getCreatedAt);
        IPage<SysLog> result = logMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量回填操作人姓名
        Set<Long> userIds = result.getRecords().stream()
            .map(SysLog::getUserId)
            .filter(Objects::nonNull)
            .filter(s -> s.matches("\\d+"))
            .map(Long::valueOf)
            .collect(Collectors.toSet());
        if (!userIds.isEmpty()) {
            List<SysUser> users = userMapper.selectBatchIds(userIds);
            Map<Long, String> nameMap = users.stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
            for (SysLog log : result.getRecords()) {
                if (log.getUserId() != null && log.getUserId().matches("\\d+")) {
                    String name = nameMap.get(Long.valueOf(log.getUserId()));
                    if (name != null && !name.isEmpty()) {
                        log.setUserId(name);
                    }
                }
            }
        }
        return result;
    }
}
