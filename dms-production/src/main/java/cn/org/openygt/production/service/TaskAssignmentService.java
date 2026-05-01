package cn.org.openygt.production.service;

import cn.org.openygt.production.dto.DeviceLoadDTO;
import cn.org.openygt.production.dto.EmployeeLoadDTO;
import cn.org.openygt.production.dto.GanttItemDTO;
import cn.org.openygt.production.entity.TaskAssignment;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskAssignmentService {
    TaskAssignment autoAssign(Long taskId, String strategy);
    TaskAssignment manualAssign(Long taskId, Long deviceId, Long employeeId, String reason);
    TaskAssignment reassign(Long assignmentId, Long newDeviceId, Long newEmployeeId, String reason);
    List<GanttItemDTO> getSchedule(LocalDateTime startTime, LocalDateTime endTime);
    List<EmployeeLoadDTO> getEmployeeLoad();
    List<DeviceLoadDTO> getDeviceLoad();
}
