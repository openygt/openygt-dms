package cn.org.openygt.analytics.service;

import cn.org.openygt.common.dto.CapacityDailyDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 产能统计报表服务。
 */
public interface CapacityReportService {

    /**
     * 查询日产能统计。
     */
    List<CapacityDailyDTO> getDailyCapacity(LocalDate startDate, LocalDate endDate, Long hospitalId);
}
