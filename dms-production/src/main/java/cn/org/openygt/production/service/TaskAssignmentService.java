package cn.org.openygt.production.service;

import cn.org.openygt.production.dto.DeviceLoadDTO;
import cn.org.openygt.production.dto.EmployeeLoadDTO;
import cn.org.openygt.production.dto.GanttItemDTO;
import cn.org.openygt.production.dto.OccupiedIds;
import cn.org.openygt.production.entity.TaskAssignment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TaskAssignmentService {
    TaskAssignment autoAssign(Long taskId, String strategy);
    TaskAssignment manualAssign(Long taskId, Long deviceId, Long employeeId, String reason, LocalDate scheduledDate);
    TaskAssignment reassign(Long assignmentId, Long newDeviceId, Long newEmployeeId, String reason);
    List<GanttItemDTO> getSchedule(LocalDateTime startTime, LocalDateTime endTime);
    List<EmployeeLoadDTO> getEmployeeLoad(LocalDate date);
    List<DeviceLoadDTO> getDeviceLoad(LocalDate date);

    /**
     * 查询指定日期已占用的任务/员工/设备ID列表
     */
    OccupiedIds getOccupiedIds(LocalDate date);
}
