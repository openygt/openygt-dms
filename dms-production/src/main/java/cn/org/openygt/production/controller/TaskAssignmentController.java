package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.dto.AutoAssignRequest;
import cn.org.openygt.production.dto.DeviceLoadDTO;
import cn.org.openygt.production.dto.EmployeeLoadDTO;
import cn.org.openygt.production.dto.GanttItemDTO;
import cn.org.openygt.production.dto.ManualAssignRequest;
import cn.org.openygt.production.dto.ReassignRequest;
import cn.org.openygt.production.entity.TaskAssignment;
import cn.org.openygt.production.service.TaskAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/assignment")
@RequiredArgsConstructor
public class TaskAssignmentController {

    private final TaskAssignmentService taskAssignmentService;

    @PostMapping("/auto")
    public ApiResponse<TaskAssignment> autoAssign(@Validated @RequestBody AutoAssignRequest req) {
        return ApiResponse.success(taskAssignmentService.autoAssign(req.getTaskId(), req.getStrategy()));
    }

    @PostMapping("/manual")
    public ApiResponse<TaskAssignment> manualAssign(@Validated @RequestBody ManualAssignRequest req) {
        return ApiResponse.success(taskAssignmentService.manualAssign(req.getTaskId(), req.getDeviceId(), req.getEmployeeId(), req.getReason()));
    }

    @PostMapping("/{assignmentId}/reassign")
    public ApiResponse<TaskAssignment> reassign(@PathVariable Long assignmentId, @Validated @RequestBody ReassignRequest req) {
        return ApiResponse.success(taskAssignmentService.reassign(assignmentId, req.getNewDeviceId(), req.getNewEmployeeId(), req.getReason()));
    }

    @GetMapping("/schedule")
    public ApiResponse<List<GanttItemDTO>> schedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ApiResponse.success(taskAssignmentService.getSchedule(startTime, endTime));
    }

    @GetMapping("/employee-load")
    public ApiResponse<List<EmployeeLoadDTO>> employeeLoad() {
        return ApiResponse.success(taskAssignmentService.getEmployeeLoad());
    }

    @GetMapping("/device-load")
    public ApiResponse<List<DeviceLoadDTO>> deviceLoad() {
        return ApiResponse.success(taskAssignmentService.getDeviceLoad());
    }
}
