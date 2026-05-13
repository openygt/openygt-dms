package cn.org.openygt.common.service;

import cn.org.openygt.common.dto.CapacityDailyDTO;
import cn.org.openygt.common.dto.ProdTaskDTO;
import cn.org.openygt.common.dto.TaskStatusHistoryDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 生产查询服务接口（只读 SPI）。
 * 定义在 dms-common，由 dms-production 实现。
 * 供 dms-quality、dms-print、dms-analytics 反查生产任务信息。
 */
public interface ProductionQueryService {

    ProdTaskDTO getTaskById(Long taskId);

    List<ProdTaskDTO> getTasksByIds(List<Long> taskIds);

    List<CapacityDailyDTO> getDailyCapacity(LocalDate startDate, LocalDate endDate, Long hospitalId);

    List<TaskStatusHistoryDTO> getTaskStatusHistory(Long taskId);
}
