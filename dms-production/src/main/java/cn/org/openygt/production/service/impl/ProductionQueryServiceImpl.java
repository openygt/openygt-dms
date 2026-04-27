package cn.org.openygt.production.service.impl;

import cn.org.openygt.common.dto.CapacityDailyDTO;
import cn.org.openygt.common.dto.ProdTaskDTO;
import cn.org.openygt.common.dto.TaskStatusHistoryDTO;
import cn.org.openygt.common.service.ProductionQueryService;
import cn.org.openygt.production.entity.Prescription;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.entity.TaskStatusHistory;
import cn.org.openygt.production.mapper.PrescriptionMapper;
import cn.org.openygt.production.mapper.TaskMapper;
import cn.org.openygt.production.mapper.TaskStatusHistoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ProductionQueryService 实现 —— 供 dms-quality、dms-print、dms-analytics 反查生产任务信息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionQueryServiceImpl implements ProductionQueryService {

    private final TaskMapper taskMapper;
    private final TaskStatusHistoryMapper historyMapper;
    private final PrescriptionMapper prescriptionMapper;

    @Override
    public ProdTaskDTO getTaskById(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        return toProdTaskDTO(task);
    }

    @Override
    public List<ProdTaskDTO> getTasksByIds(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Task> tasks = taskMapper.selectBatchIds(taskIds);
        return tasks.stream().map(this::toProdTaskDTO).collect(Collectors.toList());
    }

    @Override
    public List<CapacityDailyDTO> getDailyCapacity(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> rows = taskMapper.selectDailyCapacity(startDate.toString(), endDate.toString());
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<CapacityDailyDTO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            CapacityDailyDTO dto = new CapacityDailyDTO();
            String dateStr = (String) row.get("statDate");
            dto.setStatDate(dateStr != null ? LocalDate.parse(dateStr) : null);
            dto.setTotalTasks(((Number) row.get("totalTasks")).longValue());
            dto.setCompletedTasks(((Number) row.get("completedTasks")).longValue());
            dto.setTaskCount(((Number) row.get("totalTasks")).intValue());
            dto.setCompletedCount(((Number) row.get("completedTasks")).intValue());
            dto.setDoseCount(((Number) row.getOrDefault("doseCount", 0)).intValue());
            dto.setAvgDuration(0.0);
            dto.setDeviceUtilization(0.0);
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<TaskStatusHistoryDTO> getTaskStatusHistory(Long taskId) {
        LambdaQueryWrapper<TaskStatusHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskStatusHistory::getTaskId, taskId)
               .orderByAsc(TaskStatusHistory::getOperateTime);
        List<TaskStatusHistory> histories = historyMapper.selectList(wrapper);
        return histories.stream().map(this::toHistoryDTO).collect(Collectors.toList());
    }

    private ProdTaskDTO toProdTaskDTO(Task task) {
        if (task == null) {
            return null;
        }
        ProdTaskDTO dto = new ProdTaskDTO();
        dto.setId(task.getId());
        dto.setPrescriptionId(task.getPrescriptionId());
        dto.setStatus(task.getStatus());
        dto.setDecoctDeviceId(task.getDecoctDeviceId());
        dto.setPackageDeviceId(task.getPackageDeviceId());
        dto.setOperatorId(task.getOperatorId());
        dto.setIsException(task.getIsException());
        dto.setExceptionReason(task.getExceptionReason());
        dto.setHandoverTime(task.getHandoverTime());
        dto.setCompleteTime(task.getCompleteTime());
        dto.setCreatedAt(task.getCreatedAt());
        return dto;
    }

    private TaskStatusHistoryDTO toHistoryDTO(TaskStatusHistory history) {
        if (history == null) {
            return null;
        }
        TaskStatusHistoryDTO dto = new TaskStatusHistoryDTO();
        dto.setId(history.getId());
        dto.setTaskId(history.getTaskId());
        dto.setFromStatus(history.getFromStatus());
        dto.setToStatus(history.getToStatus());
        dto.setOperatorId(history.getOperatorId());
        dto.setOperateTime(history.getOperateTime());
        dto.setRemark(history.getRemark());
        return dto;
    }
}
