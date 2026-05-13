package cn.org.openygt.equipment.service.impl;

import cn.org.openygt.equipment.entity.WorkloadStat;
import cn.org.openygt.equipment.mapper.WorkloadStatMapper;
import cn.org.openygt.equipment.service.WorkloadStatService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkloadStatServiceImpl implements WorkloadStatService {

    private final WorkloadStatMapper statMapper;

    @Override
    public Page<WorkloadStat> queryStats(LocalDate startDate, LocalDate endDate, Long operatorId, String workType, long page, long size) {
        QueryWrapper<WorkloadStat> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        if (startDate != null) {
            wrapper.ge("stat_date", startDate);
        }
        if (endDate != null) {
            wrapper.le("stat_date", endDate);
        }
        if (operatorId != null) {
            wrapper.eq("operator_id", operatorId);
        }
        if (workType != null && !workType.isEmpty()) {
            wrapper.eq("work_type", workType);
        }
        wrapper.orderByDesc("stat_date");
        return statMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<Map<String, Object>> getSummary(LocalDate startDate, LocalDate endDate) {
        return statMapper.aggregateByType(startDate, endDate);
    }

    @Override
    public void generateDailyStats(LocalDate date) {
        QueryWrapper<WorkloadStat> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("stat_date", date);
        statMapper.delete(deleteWrapper);

        List<WorkloadStat> stats = statMapper.aggregateDailyFromTraces(date);
        for (WorkloadStat stat : stats) {
            stat.setStatDate(date);
            statMapper.insert(stat);
        }
        log.info("生成工作量统计完成: {}, 记录数={}", date, stats.size());
    }
}
