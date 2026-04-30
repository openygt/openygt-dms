package cn.org.openygt.equipment.service;

import cn.org.openygt.equipment.entity.WorkloadStat;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface WorkloadStatService {

    Page<WorkloadStat> queryStats(LocalDate startDate, LocalDate endDate, Long operatorId, String workType, long page, long size);

    List<Map<String, Object>> getSummary(LocalDate startDate, LocalDate endDate);

    void generateDailyStats(LocalDate date);
}
