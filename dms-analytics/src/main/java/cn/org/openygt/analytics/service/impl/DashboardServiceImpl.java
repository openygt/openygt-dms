package cn.org.openygt.analytics.service.impl;

import cn.org.openygt.analytics.dto.DashboardRealtimeDTO;
import cn.org.openygt.analytics.service.DashboardService;
import cn.org.openygt.common.service.EquipmentService;
import cn.org.openygt.common.service.ProductionQueryService;
import cn.org.openygt.common.service.QualityService;
import org.springframework.stereotype.Service;

/**
 * 监控大屏服务 — 空实现（骨架）。
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private final ProductionQueryService productionQueryService;
    private final EquipmentService equipmentService;
    private final QualityService qualityService;

    public DashboardServiceImpl(ProductionQueryService productionQueryService,
                                EquipmentService equipmentService,
                                QualityService qualityService) {
        this.productionQueryService = productionQueryService;
        this.equipmentService = equipmentService;
        this.qualityService = qualityService;
    }

    @Override
    public DashboardRealtimeDTO getRealtime() {
        return new DashboardRealtimeDTO();
    }
}
