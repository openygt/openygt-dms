package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.service.TaskService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/prod/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ApiResponse<IPage<Task>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Long prescriptionId,
            @RequestParam(required = false) String operatorId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(taskService.queryTasks(status, deviceId, id, prescriptionId, operatorId, startTime, endTime, page, size));
    }

    @PostMapping("/{id}/suspend")
    public ApiResponse<Task> suspend(@PathVariable Long id, @RequestBody SuspendRequest request) {
        return ApiResponse.success(taskService.suspendTask(id, request.getOperatorId(), request.getReason(), request.getSuspendType()));
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<Task> resume(@PathVariable Long id, @RequestBody ResumeRequest request) {
        return ApiResponse.success(taskService.resumeTask(id, request.getOperatorId()));
    }

    @lombok.Data
    public static class SuspendRequest {
        private String operatorId;
        private String reason;
        private Integer suspendType;
    }

    @lombok.Data
    public static class ResumeRequest {
        private String operatorId;
    }
}
