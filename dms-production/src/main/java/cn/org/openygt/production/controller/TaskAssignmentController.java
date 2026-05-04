package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.dto.AutoAssignRequest;
import cn.org.openygt.production.dto.DeviceLoadDTO;
import cn.org.openygt.production.dto.EmployeeLoadDTO;
import cn.org.openygt.production.dto.GanttItemDTO;
import cn.org.openygt.production.dto.ManualAssignRequest;
import cn.org.openygt.production.dto.OccupiedIds;
import cn.org.openygt.production.dto.ReassignRequest;
import cn.org.openygt.production.entity.TaskAssignment;
import cn.org.openygt.production.service.TaskAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import cn.org.openygt.production.ProductionModule;

@RestController
@RequestMapping(ProductionModule.API_PREFIX + "/assignment")
@RequiredArgsConstructor
public class TaskAssignmentController {

    private final TaskAssignmentService taskAssignmentService;

    @PostMapping("/auto")
    public ApiResponse<TaskAssignment> autoAssign(@Validated @RequestBody AutoAssignRequest req) {
        return ApiResponse.success(taskAssignmentService.autoAssign(req.getTaskId(), req.getStrategy()));
    }

    @PostMapping("/manual")
    public ApiResponse<TaskAssignment> manualAssign(@Validated @RequestBody ManualAssignRequest req) {
        return ApiResponse.success(taskAssignmentService.manualAssign(req.getTaskId(), req.getDeviceId(), req.getEmployeeId(), req.getReason(), req.getScheduledDate()));
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
    public ApiResponse<List<EmployeeLoadDTO>> employeeLoad(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(taskAssignmentService.getEmployeeLoad(date));
    }

    @GetMapping("/device-load")
    public ApiResponse<List<DeviceLoadDTO>> deviceLoad(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(taskAssignmentService.getDeviceLoad(date));
    }

    @GetMapping("/occupied")
    public ApiResponse<OccupiedIds> occupied(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(taskAssignmentService.getOccupiedIds(date));
    }

    @GetMapping("/available-tasks")
    public ApiResponse<List<cn.org.openygt.production.dto.TaskOptionDTO>> availableTasks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(taskAssignmentService.getAvailableTasks(date));
    }

    @GetMapping("/available-employees")
    public ApiResponse<List<EmployeeLoadDTO>> availableEmployees(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(taskAssignmentService.getAvailableEmployees(date));
    }

    @GetMapping("/available-devices")
    public ApiResponse<List<DeviceLoadDTO>> availableDevices(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(taskAssignmentService.getAvailableDevices(date));
    }
}
