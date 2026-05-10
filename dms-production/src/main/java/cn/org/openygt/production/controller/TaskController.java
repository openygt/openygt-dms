package cn.org.openygt.production.controller;

import cn.org.openygt.common.dto.ApiResponse;
import cn.org.openygt.common.enums.InspectionResultType;
import cn.org.openygt.production.entity.Prescription;
import cn.org.openygt.production.entity.Task;
import cn.org.openygt.production.mapper.PrescriptionMapper;
import cn.org.openygt.production.service.TaskService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/prod/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final PrescriptionMapper prescriptionMapper;

    @GetMapping
    public ApiResponse<IPage<Task>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Long prescriptionId,
            @RequestParam(required = false) String operatorId,
            @RequestParam(required = false) String operatorKeyword,
            @RequestParam(required = false) String prescriptionNumber,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(taskService.queryTasks(status, deviceId, id, prescriptionId, operatorId, operatorKeyword,
                prescriptionNumber, startTime, endTime, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Task> getTaskById(@PathVariable Long id) {
        Task task = taskService.getById(id);
        if (task == null) {
            return ApiResponse.error(404, "任务不存在: " + id);
        }
        return ApiResponse.success(task);
    }

    @GetMapping("/barcode/{barcode}")
    public ApiResponse<Map<String, Object>> getByBarcode(@PathVariable String barcode) {
        Task task = taskService.getByBarcode(barcode);
        if (task == null) {
            return ApiResponse.error(404, "任务不存在: " + barcode);
        }
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", task.getId());
        result.put("barcode", task.getBarcode());
        result.put("status", task.getStatus());
        result.put("prescriptionId", task.getPrescriptionId());
        String prescriptionNo = task.getPrescriptionNumber();
        if (prescriptionNo == null && task.getPrescriptionId() != null) {
            Prescription prescription = prescriptionMapper.selectById(task.getPrescriptionId());
            prescriptionNo = prescription != null ? prescription.getPrescriptionNumber() : null;
        }
        result.put("prescriptionNumber", prescriptionNo);
        return ApiResponse.success(result);
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

    /**
     * 详细质检（含检查项明细）：后端原子接口，统一完成 任务推进 + 台账写入 + 留样创建。
     */
    @PostMapping("/{id}/quality-detail")
    public ApiResponse<Task> qualityInspectWithItems(@PathVariable Long id,
                                                      @RequestBody QualityDetailRequest request,
                                                      @RequestAttribute(value = "userId", required = false) Long userId) {
        InspectionResultType resultType = InspectionResultType.valueOf(request.getResult());
        return ApiResponse.success(taskService.qualityInspectWithItems(id, resultType,
                resolveOperatorId(request.getOperatorId(), userId), request.getRemark(), request.getReworkNode(), request.getItems()));
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

    @PostMapping("/{id}/assign-operator")
    public ApiResponse<Task> assignOperator(@PathVariable Long id,
                                            @RequestBody AssignOperatorRequest request,
                                            @RequestAttribute(value = "userId", required = false) Long userId) {
        if (request == null || request.getOperatorId() == null || request.getOperatorId().trim().isEmpty()) {
            throw new IllegalArgumentException("operatorId 不能为空");
        }
        String acting = resolveOperatorId(null, userId);
        return ApiResponse.success(taskService.assignOperator(
                id, request.getOperatorId().trim(), request.getOperatorName(), acting));
    }

    private String resolveOperatorId(String operatorId, Long userId) {
        // 写操作优先以鉴权上下文中的 userId 为准，防止请求体伪造 operatorId
        if (userId != null) {
            return String.valueOf(userId);
        }
        if (operatorId != null && !operatorId.trim().isEmpty()) {
            return operatorId;
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
    public static class RollbackRequest {
        private String rollbackTo;
        private String reason;
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
    public static class QualityDetailRequest {
        private String result;
        private String operatorId;
        private String remark;
        private String reworkNode;
        private java.util.List<cn.org.openygt.common.dto.InspectionItemDTO> items;
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

    @lombok.Data
    public static class AssignOperatorRequest {
        /** 新操作人 ID（通常与系统用户 id 一致） */
        private String operatorId;
        /** 可选，展示名 */
        private String operatorName;
    }
}
