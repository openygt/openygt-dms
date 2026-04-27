package cn.org.openygt.analytics.service.impl;

import cn.org.openygt.common.dto.CapacityDailyDTO;
import cn.org.openygt.analytics.service.CapacityReportService;
import cn.org.openygt.common.service.ProductionQueryService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 产能统计报表服务 — 空实现（骨架）。
 */
@Service
public class CapacityReportServiceImpl implements CapacityReportService {

    private final ProductionQueryService productionQueryService;

    public CapacityReportServiceImpl(ProductionQueryService productionQueryService) {
        this.productionQueryService = productionQueryService;
    }

    @Override
    public List<CapacityDailyDTO> getDailyCapacity(LocalDate startDate, LocalDate endDate, Long hospitalId) {
        List<CapacityDailyDTO> list = productionQueryService.getDailyCapacity(startDate, endDate);
        // 若按医院筛选，前端做二次过滤（简化实现）
        if (hospitalId != null && list != null) {
            // TODO: 按医院过滤
        }
        return list != null ? list : Collections.emptyList();
    }
}
