package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.enums.InspectionResultType;
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
            @RequestParam(required = false) String prescriptionNumber,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(taskService.queryTasks(status, deviceId, id, prescriptionId, operatorId, prescriptionNumber, startTime, endTime, page, size));
    }

    @PostMapping("/{id}/soak/start")
    public ApiResponse<Task> startSoak(@PathVariable Long id,
                                       @RequestBody(required = false) SoakStartRequest request,
                                       @RequestAttribute(value = "userId", required = false) Long userId) {
        return ApiResponse.success(taskService.startSoak(id, resolveOperatorId(request != null ? request.getOperatorId() : null, userId)));
    }

    @PostMapping("/{id}/decoct/start")
    public ApiResponse<Task> startDecoct(@PathVariable Long id,
                                         @RequestBody(required = false) DecoctStartRequest request,
                                         @RequestAttribute(value = "userId", required = false) Long userId) {
        DecoctStartRequest actualRequest = request != null ? request : new DecoctStartRequest();
        return ApiResponse.success(taskService.startDecoct(id, actualRequest.getDeviceCode(), resolveOperatorId(actualRequest.getOperatorId(), userId)));
    }

    @PostMapping("/{id}/quality")
    public ApiResponse<Task> qualityInspect(@PathVariable Long id,
                                            @RequestBody QualityRequest request,
                                            @RequestAttribute(value = "userId", required = false) Long userId) {
        InspectionResultType resultType = InspectionResultType.valueOf(request.getResult());
        return ApiResponse.success(taskService.qualityInspect(id, resultType,
                resolveOperatorId(request.getOperatorId(), userId), request.getRemark(), request.getReworkNode()));
    }

    @PostMapping("/{id}/suspend")
    public ApiResponse<Task> suspend(@PathVariable Long id,
                                     @RequestBody(required = false) SuspendRequest request,
                                     @RequestAttribute(value = "userId", required = false) Long userId) {
        SuspendRequest actualRequest = request != null ? request : new SuspendRequest();
        return ApiResponse.success(taskService.suspendTask(id, resolveOperatorId(actualRequest.getOperatorId(), userId), actualRequest.getReason(), actualRequest.getSuspendType()));
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<Task> resume(@PathVariable Long id,
                                    @RequestBody(required = false) ResumeRequest request,
                                    @RequestAttribute(value = "userId", required = false) Long userId) {
        return ApiResponse.success(taskService.resumeTask(id, resolveOperatorId(request != null ? request.getOperatorId() : null, userId)));
    }

    private String resolveOperatorId(String operatorId, Long userId) {
        if (operatorId != null && !operatorId.trim().isEmpty()) {
            return operatorId;
        }
        if (userId != null) {
            return String.valueOf(userId);
        }
        return "SYSTEM";
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

    @lombok.Data
    public static class QualityRequest {
        private String result;
        private String operatorId;
        private String remark;
        private String reworkNode;
    }

    @lombok.Data
    public static class SoakStartRequest {
        private String operatorId;
    }

    @lombok.Data
    public static class DecoctStartRequest {
        private String deviceCode;
        private String operatorId;
    }
}
